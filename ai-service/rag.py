"""
RAG（Retrieval-Augmented Generation）增强
将商品信息、订单信息先从数据库查出来，拼接到Prompt中再发给大模型
- 识别用户意图（订单咨询/商品咨询/通用）
- 仅检索与用户问题相关的数据，避免上下文过长浪费Token
"""
import json
import logging
import re
from typing import Any

from config import get_settings
from db import query_all, query_one

logger = logging.getLogger("rag")


# 意图关键词
_ORDER_KEYWORDS = ["订单", "我的订单", "下单", "买了", "买过", "发货", "物流", "快递", "退款", "退换", "售后", "签收", "运单"]
_PRODUCT_KEYWORDS = ["商品", "这个", "多少钱", "价格", "库存", "有货吗", "盲盒", "系列", "款式", "什么样", "介绍"]
_SHOP_KEYWORDS = ["店铺", "商家", "店", "卖家"]


def _detect_intent(message: str) -> set[str]:
    """识别用户问题涉及的意图类型"""
    intents: set[str] = set()
    if any(k in message for k in _ORDER_KEYWORDS):
        intents.add("order")
    if any(k in message for k in _PRODUCT_KEYWORDS):
        intents.add("product")
    if any(k in message for k in _SHOP_KEYWORDS):
        intents.add("shop")
    return intents


def _extract_series_or_product(message: str) -> list[str]:
    """从用户问题中粗略抽取可能是系列/商品名的关键词（用于模糊匹配）"""
    # 去掉常见停用词，按非字母数字汉字切分
    keywords = re.findall(r"[\u4e00-\u9fa5A-Za-z0-9]{2,}", message)
    stop = {"怎么", "如何", "请问", "一下", "可以", "吗", "我的", "什么", "是不是", "有没有", "多少", "钱"}
    return [k for k in keywords if k not in stop and len(k) >= 2]


def fetch_recent_orders(user_id: str, limit: int) -> list[dict[str, Any]]:
    """查询用户最近订单（含订单项），作为RAG上下文"""
    sql = """
        SELECT o.order_id, o.order_no, o.order_status, o.actual_amount,
               o.payment_time, o.shipped_time, o.logistics_company, o.tracking_number,
               o.total_quantity, o.create_time
        FROM orders o
        WHERE o.user_id = %s
        ORDER BY o.create_time DESC
        LIMIT %s
    """
    orders = query_all(sql, (user_id, limit))
    if not orders:
        return []
    # 取每个订单的订单项
    order_ids = [o["order_id"] for o in orders]
    placeholders = ",".join(["%s"] * len(order_ids))
    item_sql = f"""
        SELECT order_id, product_name, product_spec, quantity, unit_price, actual_subtotal
        FROM order_items
        WHERE order_id IN ({placeholders})
    """
    items = query_all(item_sql, tuple(order_ids))
    items_map: dict[str, list[dict[str, Any]]] = {}
    for it in items:
        items_map.setdefault(it["order_id"], []).append(it)
    for o in orders:
        o["items"] = items_map.get(o["order_id"], [])
        # 时间字段序列化
        for k, v in list(o.items()):
            if hasattr(v, "strftime"):
                o[k] = v.strftime("%Y-%m-%d %H:%M:%S")
    return orders


def search_products(message: str, limit: int) -> list[dict[str, Any]]:
    """
    商品检索：先按关键词模糊匹配，再按热度排序
    简化版RAG（向量检索需引入embedding，此处用MySQL全文/模糊检索足够）
    """
    keywords = _extract_series_or_product(message)
    if not keywords:
        # 没有关键词时，返回热销商品
        sql = """
            SELECT p.product_id, p.name, p.price, p.stock, p.status, p.brand,
                   p.series_id, s.series_name, s.theme, s.cover_image, s.series_hotness
            FROM product p
            LEFT JOIN series s ON p.series_id = s.series_id
            WHERE p.status = 'ON_SALE'
            ORDER BY p.market_hotness DESC
            LIMIT %s
        """
        return query_all(sql, (limit,))

    # 关键词LIKE匹配（任一关键词命中）
    where_parts = []
    args: list[Any] = []
    for kw in keywords[:3]:  # 最多用前3个关键词，避免SQL过长
        where_parts.append("(p.name LIKE %s OR p.description LIKE %s OR s.series_name LIKE %s OR s.theme LIKE %s)")
        args.extend([f"%{kw}%", f"%{kw}%", f"%{kw}%", f"%{kw}%"])
    where_clause = " OR ".join(where_parts)
    sql = f"""
        SELECT p.product_id, p.name, p.price, p.stock, p.status, p.brand,
               p.series_id, s.series_name, s.theme, s.cover_image, s.series_hotness
        FROM product p
        LEFT JOIN series s ON p.series_id = s.series_id
        WHERE p.status = 'ON_SALE' AND ({where_clause})
        ORDER BY s.series_hotness DESC
        LIMIT %s
    """
    args.append(limit)
    return query_all(sql, tuple(args))


def fetch_series_info(series_id: str) -> dict[str, Any] | None:
    """查询单个系列的详细信息"""
    sql = """
        SELECT series_id, series_name, theme, description, cover_image,
               min_price, fullset_price, status, series_hotness,
               regular_variants, hidden_variants, is_limited
        FROM series WHERE series_id = %s
    """
    row = query_one(sql, (series_id,))
    if row and row.get("cover_image"):
        # cover_image可能为JSON数组字符串，保留原样
        pass
    return row


def build_rag_context(user_id: str, message: str) -> str:
    """
    根据用户问题构建RAG上下文文本，拼接到Prompt
    只检索相关数据，控制上下文长度
    """
    cfg = get_settings()
    intents = _detect_intent(message)
    sections: list[str] = []

    if "order" in intents and user_id:
        orders = fetch_recent_orders(user_id, cfg.RAG_RECENT_ORDER_LIMIT)
        if orders:
            lines = ["【用户近期订单信息】（仅用于本次客服回答，请勿泄露给其他用户）"]
            for o in orders:
                items_str = "; ".join(
                    f"{it['product_name']}×{it['quantity']}(¥{it['unit_price']})"
                    for it in o.get("items", [])
                )
                lines.append(
                    f"- 订单号 {o['order_no']} | 状态:{o['order_status']} | "
                    f"实付¥{o['actual_amount']} | 下单时间:{o.get('create_time','')} | "
                    f"物流:{o.get('logistics_company','') or '-'} {o.get('tracking_number','') or ''} | "
                    f"商品:{items_str}"
                )
            sections.append("\n".join(lines))
        else:
            sections.append("【用户近期订单信息】用户暂无订单记录。")

    if "product" in intents:
        products = search_products(message, cfg.RAG_PRODUCT_LIMIT)
        if products:
            lines = ["【相关商品信息】"]
            for p in products:
                series_name = p.get("series_name") or "-"
                theme = p.get("theme") or "-"
                lines.append(
                    f"- 商品:{p['name']} | 系列:{series_name} | 主题:{theme} | "
                    f"价格¥{p.get('price','-')} | 库存{p.get('stock','-')} | "
                    f"商品ID:{p['product_id']}"
                )
            sections.append("\n".join(lines))

    if not sections:
        return ""
    return "\n\n".join(sections)
