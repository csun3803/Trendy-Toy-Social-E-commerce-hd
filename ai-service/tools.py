"""
Function Call 工具函数
- 每个工具函数通过 HTTP 调用 Java 后端的 /api/ai/tools/* 接口获取实时数据
- 返回的数据会被拼装成 tool 消息回传给大模型，由大模型生成最终回复
- 知识库只放静态知识（FAQ/百科/售后政策），实时数据全部走这里

Java 接口约定返回: {"code": 200, "message": "success", "data": ...}
本模块提取 data 字段返回；非 200 时返回 {"error": "..."} 便于大模型识别失败
"""
import logging
from typing import Any

import httpx

from config import get_settings

logger = logging.getLogger("tools")


def _java_get(path: str, params: dict[str, str]) -> Any:
    """统一调用 Java /api/ai/tools/* 接口并提取 data 字段"""
    cfg = get_settings()
    url = f"{cfg.JAVA_BACKEND_URL.rstrip('/')}{path}"
    try:
        with httpx.Client(timeout=cfg.TOOLS_HTTP_TIMEOUT) as client:
            resp = client.get(url, params=params)
            resp.raise_for_status()
            payload = resp.json()
    except httpx.HTTPError as e:
        logger.warning("Java工具接口HTTP异常 path=%s params=%s err=%s", path, params, e)
        return {"error": f"后端服务暂时不可用: {type(e).__name__}"}
    except Exception as e:
        logger.exception("Java工具接口解析异常 path=%s", path)
        return {"error": f"解析响应失败: {type(e).__name__}"}

    code = payload.get("code")
    if code != 200:
        msg = payload.get("message") or "后端业务错误"
        logger.info("Java工具接口业务错误 path=%s code=%s msg=%s", path, code, msg)
        return {"error": msg}
    return payload.get("data")


# ===================== 工具函数实现 =====================

def query_logistics(order_id: str) -> Any:
    """
    查询订单物流信息。
    当用户问“订单到哪了 / 我的快递到哪了 / 物流进度”时调用。
    :param order_id: 订单ID（或订单号，Java 端按 orderId 处理）
    """
    order_id = (order_id or "").strip()
    if not order_id:
        return {"error": "缺少订单ID，请让用户提供订单号"}
    return _java_get("/api/ai/tools/logistics", {"orderId": order_id})


def query_series_info(series_name: str) -> Any:
    """
    查询系列摘要信息（名称、ID、款式数、封面等）。
    当用户问“XX系列”、“XX款式”、“XXIP”时调用。
    返回的结果同时会被收集为「卡片」回传给前端，前端渲染为可点击卡片，点击跳转 /series/{seriesId}。
    :param series_name: 系列名称/IP名/主题（支持模糊匹配）
    """
    series_name = (series_name or "").strip()
    if not series_name:
        return {"error": "缺少系列名称，请让用户提供系列名"}
    return _java_get("/api/ai/tools/series/info", {"seriesName": series_name})


def query_series_styles(series_name: str) -> Any:
    """
    查询某个系列下的款式列表。
    当用户问“XX系列有哪些款式 / XX系列包含什么”时调用。
    :param series_name: 系列名称（支持模糊匹配）
    """
    series_name = (series_name or "").strip()
    if not series_name:
        return {"error": "缺少系列名称，请让用户提供系列名"}
    return _java_get("/api/ai/tools/series/styles", {"seriesName": series_name})


def query_user_orders(user_id: str) -> Any:
    """
    查询用户订单列表。
    当用户问“我的订单 / 我买了什么 / 最近的订单”时调用。
    :param user_id: 用户ID
    """
    user_id = (user_id or "").strip()
    if not user_id:
        return {"error": "缺少用户ID，无法查询订单"}
    return _java_get("/api/ai/tools/orders", {"userId": user_id})


def query_user_favorites(user_id: str) -> Any:
    """
    查询用户收藏的商品列表。
    当用户问“我的收藏 / 我收藏了什么”时调用。
    :param user_id: 用户ID
    """
    user_id = (user_id or "").strip()
    if not user_id:
        return {"error": "缺少用户ID，无法查询收藏"}
    return _java_get("/api/ai/tools/favorites", {"userId": user_id})


def query_user_draw_history(user_id: str) -> Any:
    """
    查询用户抽盒（盲盒）历史记录。
    当用户问“我抽过哪些 / 我的抽盒记录 / 我抽中了什么”时调用。
    :param user_id: 用户ID
    """
    user_id = (user_id or "").strip()
    if not user_id:
        return {"error": "缺少用户ID，无法查询抽盒历史"}
    return _java_get("/api/ai/tools/draw-history", {"userId": user_id})


def query_style_stock(style_name: str) -> Any:
    """
    查询某个款式的库存情况。
    当用户问“XX款还有货吗 / XX款有库存吗”时调用。
    :param style_name: 款式名称（支持模糊匹配）
    """
    style_name = (style_name or "").strip()
    if not style_name:
        return {"error": "缺少款式名称，请让用户提供款式名"}
    return _java_get("/api/ai/tools/style-stock", {"styleName": style_name})


# ===================== 工具函数 Schema（供智谱 Function Call 使用） =====================
# 采用 OpenAI 兼容格式，智谱 GLM-4 系列支持 tools 参数

TOOL_SCHEMAS: list[dict[str, Any]] = [
    {
        "type": "function",
        "function": {
            "name": "query_logistics",
            "description": "查询订单物流信息。当用户询问订单物流、快递到哪了、物流进度、快递单号时调用。",
            "parameters": {
                "type": "object",
                "properties": {
                    "order_id": {
                        "type": "string",
                        "description": "订单ID或订单号",
                    }
                },
                "required": ["order_id"],
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "query_series_info",
            "description": "查询系列摘要信息（系列ID、名称、款式数、封面、主题等）。当用户询问“XX系列”、“XX款式”、“XXIP”、“告诉我XX系列”时调用。返回结果会同时作为卡片回传前端，用户可点击查看系列详情。",
            "parameters": {
                "type": "object",
                "properties": {
                    "series_name": {
                        "type": "string",
                        "description": "系列名称/IP名/主题，支持模糊匹配，例如“LABUBU”、“温暖”、“动物宇宙”",
                    }
                },
                "required": ["series_name"],
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "query_series_styles",
            "description": "查询某个系列下的款式列表。当用户询问某系列有哪些款式、包含哪些盲盒款式时调用。",
            "parameters": {
                "type": "object",
                "properties": {
                    "series_name": {
                        "type": "string",
                        "description": "系列名称，支持模糊匹配，例如“温暖系列”、“动物宇宙”",
                    }
                },
                "required": ["series_name"],
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "query_user_orders",
            "description": "查询用户的订单列表。当用户询问“我的订单”、“我买了什么”、“最近订单”时调用。",
            "parameters": {
                "type": "object",
                "properties": {
                    "user_id": {
                        "type": "string",
                        "description": "用户ID",
                    }
                },
                "required": ["user_id"],
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "query_user_favorites",
            "description": "查询用户收藏的商品列表。当用户询问“我的收藏”、“我收藏了什么”时调用。",
            "parameters": {
                "type": "object",
                "properties": {
                    "user_id": {
                        "type": "string",
                        "description": "用户ID",
                    }
                },
                "required": ["user_id"],
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "query_user_draw_history",
            "description": "查询用户抽盒（盲盒）历史记录。当用户询问“我抽过哪些”、“我的抽盒记录”、“我抽中了什么”时调用。",
            "parameters": {
                "type": "object",
                "properties": {
                    "user_id": {
                        "type": "string",
                        "description": "用户ID",
                    }
                },
                "required": ["user_id"],
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "query_style_stock",
            "description": "查询某个款式的库存情况。当用户询问“XX款还有货吗”、“有库存吗”、“还能买吗”时调用。",
            "parameters": {
                "type": "object",
                "properties": {
                    "style_name": {
                        "type": "string",
                        "description": "款式名称，支持模糊匹配",
                    }
                },
                "required": ["style_name"],
            },
        },
    },
]

# 工具函数名 -> 可调用对象的映射表
TOOL_FUNCTIONS: dict[str, Any] = {
    "query_logistics": query_logistics,
    "query_series_info": query_series_info,
    "query_series_styles": query_series_styles,
    "query_user_orders": query_user_orders,
    "query_user_favorites": query_user_favorites,
    "query_user_draw_history": query_user_draw_history,
    "query_style_stock": query_style_stock,
}

# 调用结果需要被收集为「卡片」回传前端的工具集合。
# 这些工具返回的数据除供大模型生成文本外，还会作为结构化卡片数据回传前端渲染。
# 卡片格式由 build_cards_from_tool_result 统一构造。
CARD_PRODUCING_TOOLS: set[str] = {"query_series_info"}


def build_cards_from_tool_result(tool_name: str, tool_result: Any) -> list[dict[str, Any]]:
    """
    将工具返回结果转换为前端可渲染的卡片列表。
    目前仅 query_series_info 产生系列卡片：
        [{ type: "series", seriesId, seriesName, variantCount, coverImage, theme }]
    """
    cards: list[dict[str, Any]] = []
    if tool_name != "query_series_info":
        return cards
    if isinstance(tool_result, dict):
        # 单个系列对象或 {"error": ...}
        if "error" in tool_result:
            return cards
        rows = [tool_result]
    elif isinstance(tool_result, list):
        rows = tool_result
    else:
        return cards

    for row in rows:
        if not isinstance(row, dict):
            continue
        series_id = row.get("seriesId") or row.get("series_id")
        if not series_id:
            continue
        card: dict[str, Any] = {
            "type": "series",
            "seriesId": series_id,
            "seriesName": row.get("seriesName") or row.get("series_name") or "",
            "variantCount": row.get("variantCount") or row.get("totalVariants") or 0,
            "coverImage": row.get("coverImage") or row.get("cover_image") or "",
            "theme": row.get("theme") or "",
        }
        cards.append(card)
    return cards


def execute_tool(tool_name: str, arguments: dict[str, Any]) -> Any:
    """
    执行工具函数（供 customer_service 调用）
    :param tool_name: 工具函数名
    :param arguments: 大模型返回的参数字典
    :return: 工具函数返回值（字典/列表）
    """
    func = TOOL_FUNCTIONS.get(tool_name)
    if func is None:
        return {"error": f"未知的工具函数: {tool_name}"}
    try:
        return func(**arguments)
    except TypeError as e:
        logger.warning("工具参数错误 tool=%s args=%s err=%s", tool_name, arguments, e)
        return {"error": f"参数不匹配: {e}"}
    except Exception as e:
        logger.exception("工具执行异常 tool=%s", tool_name)
        return {"error": f"工具执行失败: {type(e).__name__}: {e}"}
