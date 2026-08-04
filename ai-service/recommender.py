"""
智能推荐算法
混合策略 (Hybrid Recommendation)：
  1. 内容召回 (Content-Based)：基于用户已收藏/购买/浏览过的系列的IP、主题、价位，召回同特征系列
  2. 协同过滤召回 (User-Based CF)：找相似用户，推荐他们喜欢但当前用户未交互的系列
  3. 热度兜底 (Popularity)：冷启动用户/召回不足时，用全局热销系列补足

最终用加权打分排序：
  score = α * content_score + β * cf_score + γ * popularity_score - λ * seen_penalty
  默认权重: α=0.45, β=0.30, γ=0.25，已交互过的系列打折

这样不同用户访问首页，看到的商品流/推荐位不同。
"""
import logging
import math
from collections import defaultdict
from typing import Any

from db import query_all

logger = logging.getLogger("recommender")

# 权重
W_CONTENT = 0.45
W_CF = 0.30
W_POPULARITY = 0.25
# 已交互系列的惩罚系数（避免重复推荐已买/已收藏的）
SEEN_PENALTY = 0.85
# 价位分段（用于内容相似度）
PRICE_BUCKETS = [(0, 50), (50, 100), (100, 200), (200, 500), (500, float("inf"))]


def _price_bucket(price: float | int | None) -> int:
    if price is None:
        return 0
    for i, (lo, hi) in enumerate(PRICE_BUCKETS):
        if lo <= price < hi:
            return i + 1
    return 0


def _series_to_dict(row: dict[str, Any]) -> dict[str, Any]:
    """数据库行 -> 前端Series对象（驼峰）"""
    return {
        "seriesId": row.get("series_id"),
        "seriesName": row.get("series_name"),
        "description": row.get("description"),
        "coverImage": row.get("cover_image"),
        "minPrice": float(row["min_price"]) if row.get("min_price") is not None else None,
        "fullsetPrice": float(row["fullset_price"]) if row.get("fullset_price") is not None else None,
        "status": row.get("status"),
        "theme": row.get("theme"),
        "seriesHotness": row.get("series_hotness") or 0,
        "salesCount": row.get("sales_count") or 0,
        "ipAlbumId": row.get("ip_album_id"),
    }


def _fetch_user_interactions(user_id: str) -> tuple[set[str], dict[str, set[str]]]:
    """
    获取用户已交互过的所有系列ID，以及用户画像特征 (themes, ipAlbumIds)
    数据来源：收藏、购买、浏览（user_interaction表）
    返回 (series_ids, {"theme": set, "ip": set, "price_bucket": set})
    """
    series_ids: set[str] = set()
    profile: dict[str, set] = {"theme": set(), "ip": set(), "price_bucket": set()}

    # 1. 收藏商品 -> series（通过 user_interaction 表）
    fav_rows = query_all(
        """SELECT DISTINCT s.series_id, s.theme, s.ip_album_id, s.min_price
           FROM user_interaction ui
           JOIN sale_variant sv ON ui.target_id = sv.sale_variant_id
           LEFT JOIN sale_series ss ON sv.sale_series_id = ss.sale_series_id
           LEFT JOIN series s ON ss.series_id = s.series_id
           WHERE ui.user_id = %s AND ui.action_type = 'FAVORITE' AND ui.target_type = 'PRODUCT'
             AND ui.status = 'ACTIVE' AND s.series_id IS NOT NULL""",
        (user_id,),
    )
    for r in fav_rows:
        series_ids.add(r["series_id"])
        if r.get("theme"):
            profile["theme"].add(r["theme"])
        if r.get("ip_album_id"):
            profile["ip"].add(r["ip_album_id"])
        profile["price_bucket"].add(_price_bucket(float(r["min_price"]) if r.get("min_price") else 0))

    # 2. 购买 -> series（通过订单）
    bought_rows = query_all(
        """SELECT DISTINCT s.series_id, s.theme, s.ip_album_id, s.min_price
           FROM order_items oi
           JOIN orders o ON oi.order_id = o.order_id
           LEFT JOIN sale_variant sv ON oi.product_id = sv.sale_variant_id
           LEFT JOIN sale_series ss ON sv.sale_series_id = ss.sale_series_id
           LEFT JOIN series s ON ss.series_id = s.series_id
           WHERE o.user_id = %s AND s.series_id IS NOT NULL""",
        (user_id,),
    )
    for r in bought_rows:
        series_ids.add(r["series_id"])
        if r.get("theme"):
            profile["theme"].add(r["theme"])
        if r.get("ip_album_id"):
            profile["ip"].add(r["ip_album_id"])
        profile["price_bucket"].add(_price_bucket(float(r["min_price"]) if r.get("min_price") else 0))

    # 3. 浏览商品（user_interaction 表 VIEW 动作）-> series
    browse_rows = query_all(
        """SELECT DISTINCT sv.sale_series_id AS series_id
           FROM user_interaction ui
           JOIN sale_variant sv ON ui.target_id = sv.sale_variant_id
           WHERE ui.user_id = %s AND ui.action_type = 'VIEW' AND ui.target_type = 'PRODUCT'
             AND ui.status = 'ACTIVE' AND sv.sale_series_id IS NOT NULL""",
        (user_id,),
    )
    browsed_series_ids = [r["series_id"] for r in browse_rows if r.get("series_id")]
    for sid in browsed_series_ids:
        series_ids.add(sid)

    # 浏览过的系列也加入画像
    if browsed_series_ids:
        placeholders = ",".join(["%s"] * len(browsed_series_ids))
        srows = query_all(
            f"SELECT series_id, theme, ip_album_id, min_price FROM sale_series WHERE series_id IN ({placeholders})",
            tuple(browsed_series_ids),
        )
        for r in srows:
            if r.get("theme"):
                profile["theme"].add(r["theme"])
            if r.get("ip_album_id"):
                profile["ip"].add(r["ip_album_id"])
            profile["price_bucket"].add(_price_bucket(float(r["min_price"]) if r.get("min_price") else 0))

    return series_ids, profile


def _content_candidates(profile: dict[str, set], exclude: set[str], limit: int) -> list[dict[str, Any]]:
    """内容召回：基于用户画像中的theme/ip/price_bucket召回候选系列"""
    if not profile["theme"] and not profile["ip"]:
        return []

    # 拼OR条件：theme命中 OR ip命中
    sql = "SELECT * FROM series WHERE status = 'ON_SALE'"
    args: list[Any] = []
    conds = []
    if profile["theme"]:
        placeholders = ",".join(["%s"] * len(profile["theme"]))
        conds.append(f"theme IN ({placeholders})")
        args.extend(list(profile["theme"]))
    if profile["ip"]:
        placeholders = ",".join(["%s"] * len(profile["ip"]))
        conds.append(f"ip_album_id IN ({placeholders})")
        args.extend(list(profile["ip"]))
    sql += " AND (" + " OR ".join(conds) + ")"

    if exclude:
        placeholders = ",".join(["%s"] * len(exclude))
        sql += f" AND series_id NOT IN ({placeholders})"
        args.extend(list(exclude))

    sql += " ORDER BY series_hotness DESC LIMIT %s"
    args.append(limit)
    return query_all(sql, tuple(args))


def _cf_candidates(user_id: str, exclude: set[str], limit: int) -> tuple[list[dict[str, Any]], dict[str, float]]:
    """
    User-Based 协同过滤召回
    1. 找到与当前用户交互系列重叠度最高的N个相似用户
    2. 把这些相似用户交互过、但当前用户没交互过的系列召回
    返回 (候选series_id列表, series_id->cf_score映射)
    """
    # 当前用户的交互系列（通过 user_interaction + 订单）
    user_series_rows = query_all(
        """SELECT DISTINCT ss.series_id FROM (
              SELECT sv.sale_series_id AS series_id
              FROM user_interaction ui
              JOIN sale_variant sv ON ui.target_id = sv.sale_variant_id
              WHERE ui.user_id = %s AND ui.target_type = 'PRODUCT' AND ui.status = 'ACTIVE'
              UNION
              SELECT sv.sale_series_id AS series_id
              FROM order_items oi
              JOIN orders o ON oi.order_id = o.order_id
              JOIN sale_variant sv ON oi.product_id = sv.sale_variant_id
              WHERE o.user_id = %s
           ) ss WHERE ss.series_id IS NOT NULL""",
        (user_id, user_id),
    )
    user_series = {r["series_id"] for r in user_series_rows}
    if not user_series:
        return [], {}

    # 找出也交互过这些系列的其他用户（Jaccard相似度），取Top N
    placeholders = ",".join(["%s"] * len(user_series))
    similar_users_rows = query_all(
        f"""SELECT other.user_id, COUNT(DISTINCT other.series_id) AS overlap
            FROM (
              SELECT ui.user_id, sv.sale_series_id AS series_id
              FROM user_interaction ui
              JOIN sale_variant sv ON ui.target_id = sv.sale_variant_id
              WHERE ui.target_type = 'PRODUCT' AND ui.status = 'ACTIVE'
                AND sv.sale_series_id IN ({placeholders}) AND ui.user_id <> %s
              UNION
              SELECT o.user_id, sv.sale_series_id AS series_id
              FROM order_items oi
              JOIN orders o ON oi.order_id = o.order_id
              JOIN sale_variant sv ON oi.product_id = sv.sale_variant_id
              WHERE sv.sale_series_id IN ({placeholders}) AND o.user_id <> %s
            ) other
            GROUP BY other.user_id
            ORDER BY overlap DESC
            LIMIT 50""",
        tuple(list(user_series) + [user_id] + list(user_series) + [user_id]),
    )

    if not similar_users_rows:
        return [], {}

    similar_users = [r["user_id"] for r in similar_users_rows]
    overlap_map = {r["user_id"]: r["overlap"] for r in similar_users_rows}

    # 召回相似用户交互过但当前用户没交互过的系列，按"被多少相似用户交互"打分
    su_placeholders = ",".join(["%s"] * len(similar_users))
    cand_rows = query_all(
        f"""SELECT series_id, COUNT(DISTINCT user_id) AS user_count
            FROM (
              SELECT ui.user_id, sv.sale_series_id AS series_id
              FROM user_interaction ui
              JOIN sale_variant sv ON ui.target_id = sv.sale_variant_id
              WHERE ui.target_type = 'PRODUCT' AND ui.status = 'ACTIVE'
                AND ui.user_id IN ({su_placeholders})
              UNION
              SELECT o.user_id, sv.sale_series_id AS series_id
              FROM order_items oi
              JOIN orders o ON oi.order_id = o.order_id
              JOIN sale_variant sv ON oi.product_id = sv.sale_variant_id
              WHERE o.user_id IN ({su_placeholders})
            ) t
            WHERE series_id IS NOT NULL
            GROUP BY series_id""",
        tuple(similar_users + similar_users),
    )

    # 计算cf_score = sum(overlap(u, current)) for u in users who interacted with this series / total_overlap
    cf_scores: dict[str, float] = {}
    for r in cand_rows:
        sid = r["series_id"]
        if sid in exclude or sid in user_series:
            continue
        # 用 user_count 与总相似用户数的比例作为分数
        cf_scores[sid] = r["user_count"] / max(len(similar_users), 1)

    return [], cf_scores


def _fetch_series_by_ids(series_ids: list[str]) -> dict[str, dict[str, Any]]:
    if not series_ids:
        return {}
    placeholders = ",".join(["%s"] * len(series_ids))
    rows = query_all(
        f"SELECT * FROM series WHERE series_id IN ({placeholders})",
        tuple(series_ids),
    )
    return {r["series_id"]: r for r in rows}


def _normalize_hotness(rows: list[dict[str, Any]]) -> dict[str, float]:
    if not rows:
        return {}
    max_h = max((r.get("series_hotness") or 0) for r in rows) or 1
    return {r["series_id"]: (r.get("series_hotness") or 0) / max_h for r in rows}


def recommend_for_user(user_id: str, limit: int = 10) -> list[dict[str, Any]]:
    """
    个性化推荐：内容召回 + 协同过滤 + 热度兜底
    """
    if limit <= 0:
        limit = 10

    seen_ids, profile = _fetch_user_interactions(user_id)

    # 召回阶段
    content_limit = max(limit * 4, 40)
    content_rows = _content_candidates(profile, seen_ids, content_limit)

    _, cf_scores = _cf_candidates(user_id, seen_ids, limit * 4)

    # 合并候选
    candidate_ids: set[str] = {r["series_id"] for r in content_rows}
    candidate_ids.update(cf_scores.keys())

    if not candidate_ids:
        # 冷启动 -> 热门
        return _hot_recommend(limit)

    cand_map = _fetch_series_by_ids(list(candidate_ids))
    if not cand_map:
        return _hot_recommend(limit)

    rows = list(cand_map.values())
    pop_map = _normalize_hotness(rows)

    # 评分
    scored: list[tuple[float, dict[str, Any]]] = []
    for sid, row in cand_map.items():
        # 内容分：theme/ip/price_bucket命中加分
        content_score = 0.0
        if row.get("theme") and row["theme"] in profile["theme"]:
            content_score += 0.6
        if row.get("ip_album_id") and row["ip_album_id"] in profile["ip"]:
            content_score += 0.4
        pb = _price_bucket(float(row["min_price"]) if row.get("min_price") else 0)
        if pb and pb in profile["price_bucket"]:
            content_score += 0.2
        content_score = min(content_score, 1.0)

        cf_score = cf_scores.get(sid, 0.0)
        pop_score = pop_map.get(sid, 0.0)

        final = (W_CONTENT * content_score
                 + W_CF * cf_score
                 + W_POPULARITY * pop_score)

        # 已浏览过的打折（不直接排除，因为浏览过不代表完全没兴趣）
        if sid in seen_ids:
            final *= SEEN_PENALTY

        scored.append((final, row))

    scored.sort(key=lambda x: x[0], reverse=True)
    picked = [r for _, r in scored[:limit]]

    # 不够则用热门补
    if len(picked) < limit:
        exclude = {r["series_id"] for r in picked}
        hot_extra = _hot_recommend(limit - len(picked), exclude)
        picked.extend(hot_extra)

    return [_series_to_dict(r) for r in picked]


def recommend_similar(series_id: str, limit: int = 6) -> list[dict[str, Any]]:
    """相似系列推荐：同IP > 同主题 > 同价位"""
    if limit <= 0:
        limit = 6
    rows = query_all(
        "SELECT * FROM series WHERE series_id = %s",
        (series_id,),
    )
    if not rows:
        return []
    current = rows[0]

    exclude = {series_id}
    result: list[dict[str, Any]] = []

    def _fetch(conds: list[str], args: list[Any], lim: int) -> list[dict[str, Any]]:
        sql = f"SELECT * FROM series WHERE status='ON_SALE' AND {' AND '.join(conds)} ORDER BY series_hotness DESC LIMIT %s"
        return query_all(sql, tuple(args + [lim]))

    # 同IP
    if current.get("ip_album_id"):
        rows2 = _fetch(
            ["ip_album_id = %s", "series_id <> %s"],
            [current["ip_album_id"], series_id],
            limit,
        )
        for r in rows2:
            if r["series_id"] not in exclude:
                result.append(r)
                exclude.add(r["series_id"])

    # 同主题
    if len(result) < limit and current.get("theme"):
        rows2 = _fetch(
            ["theme = %s", f"series_id NOT IN ({','.join(['%s']*len(exclude))})" if exclude else "1=1"],
            [current["theme"]] + list(exclude),
            limit - len(result),
        )
        for r in rows2:
            if r["series_id"] not in exclude:
                result.append(r)
                exclude.add(r["series_id"])

    # 同价位
    if len(result) < limit and current.get("min_price"):
        try:
            price = float(current["min_price"])
            rows2 = _fetch(
                ["min_price BETWEEN %s AND %s", f"series_id NOT IN ({','.join(['%s']*len(exclude))})" if exclude else "1=1"],
                [price * 0.5, price * 2] + list(exclude),
                limit - len(result),
            )
            for r in rows2:
                if r["series_id"] not in exclude:
                    result.append(r)
                    exclude.add(r["series_id"])
        except (TypeError, ValueError):
            pass

    # 热门补
    if len(result) < limit:
        hot = _hot_recommend(limit - len(result), exclude)
        result.extend(hot)

    return [_series_to_dict(r) for r in result[:limit]]


def _hot_recommend(limit: int, exclude: set[str] | None = None) -> list[dict[str, Any]]:
    """热门推荐：基于销量+热度"""
    exclude = exclude or set()
    sql = "SELECT * FROM series WHERE status='ON_SALE'"
    args: list[Any] = []
    if exclude:
        placeholders = ",".join(["%s"] * len(exclude))
        sql += f" AND series_id NOT IN ({placeholders})"
        args.extend(list(exclude))
    sql += " ORDER BY series_hotness DESC LIMIT %s"
    args.append(limit)
    return query_all(sql, tuple(args))


def hot_recommend(limit: int = 10) -> list[dict[str, Any]]:
    if limit <= 0:
        limit = 10
    return [_series_to_dict(r) for r in _hot_recommend(limit)]


def record_behavior(
    user_id: str,
    behavior_type: str,
    target_type: str,
    target_id: str,
    weight: int = 1,
) -> None:
    """
    记录用户行为到 user_interaction 表
    :param user_id: 用户ID
    :param behavior_type: 行为类型 (VIEW, FAVORITE, LIKE 等)
    :param target_type: 目标类型 (PRODUCT, POST 等)
    :param target_id: 目标ID
    :param weight: 权重 (暂不使用，预留扩展)
    """
    import uuid
    from db import execute

    interaction_id = f"int_{uuid.uuid4().hex[:16]}"
    execute(
        """INSERT INTO user_interaction
           (interaction_id, user_id, action_type, target_type, target_id, status, created_at)
           VALUES (%s, %s, %s, %s, %s, 'ACTIVE', NOW())
           ON DUPLICATE KEY UPDATE
           status = 'ACTIVE',
           created_at = NOW()""",
        (interaction_id, user_id, behavior_type, target_type, target_id),
    )
