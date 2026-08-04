"""
智能客服编排层
流程：本地FAQ优先（静态知识） -> Function Call（实时数据）-> 智谱ChatGLM兜底
设计目的：
  - 静态知识（FAQ/百科/售后政策）走本地知识库，省Token且响应快
  - 实时数据（订单/物流/款式/库存/收藏/抽盒历史）全部走 Function Call，
    通过调用 Java 后端 /api/ai/tools/* 接口获取
"""
import json
import logging
import uuid
from datetime import datetime
from typing import Any

from config import get_settings
from db import execute, query_all
from faq_kb import DEFAULT_REPLY, match_faq
from tools import TOOL_SCHEMAS, build_cards_from_tool_result, execute_tool
from zhipu_client import chat_with_tools, parse_tool_arguments

logger = logging.getLogger("customer_service")

# 涉及用户私有数据的工具，强制使用当前登录用户的 userId，防止越权查询
_USER_SPECIFIC_TOOLS = {"query_user_orders", "query_user_favorites", "query_user_draw_history"}

# 查询类工具（series_info / series_styles），需要校验参数合理性，防止误调用
_QUERY_TOOLS_REQUIRING_VALIDATION = {"query_series_info", "query_series_styles"}

# 非系列查询关键词黑名单：包含这些关键词的问题不应调用系列查询工具
_NON_SERIES_KEYWORDS = {
    "账号", "注销", "退换", "退货", "退款", "支付", "发货", "物流", "快递",
    "售后", "投诉", "优惠券", "注册", "登录", "密码", "入驻", "开店",
    "会员", "隐私", "规则", "政策", "流程", "怎么退", "怎么付", "怎么开",
    "怎么注册", "怎么登录", "忘记密码",
}

# 知识库问题关键词：包含这些关键词的问题优先走知识库检索（而非函数工具）
_KB_PRIORITY_KEYWORDS = {
    "注销", "退换货", "退换", "退款政策", "退货政策", "售后规则", "售后政策",
    "支付方式", "盲盒规则", "优惠券使用", "商家入驻", "开店", "会员权益",
    "隐私政策", "投诉流程", "平台介绍", "操作指南", "退款流程", "退货流程",
    "退款规则", "退换规则", "怎么退", "怎么付", "怎么开", "怎么注册",
    "怎么登录", "忘记密码", "入驻流程", "注册流程", "登录不了",
    "保底", "整盒", "隐藏款概率", "抽盒规则",
}

# 实时数据问题关键词：包含这些关键词的问题优先走函数工具
_REALTIME_PRIORITY_KEYWORDS = {
    "订单状态", "物流进度", "快递到哪", "发货了吗", "我的订单",
    "库存", "还有货", "款式列表", "系列信息", "我的收藏", "抽盒记录",
    "我买了什么", "LABUBU", "动物宇宙", "抽盒机", "盲盒机",
}

SYSTEM_PROMPT = (
    "你是「潮玩星球」电商平台的AI智能客服助手。\n"
    "你的职责：友好、专业、简洁地解答用户关于订单、商品、盲盒、发货、退款、售后等问题。\n"
    "\n"
    "【重要：路由规则——判断何时用知识库、何时用工具】\n"
    "你必须先判断用户问题的类型，再决定回答方式：\n"
    "\n"
    "类型A - 政策/规则类（走知识库，不调用工具）：\n"
    "  包括：账号注销、退换货政策、售后规则、支付方式、盲盒规则、优惠券使用、商家入驻、\n"
    "  会员权益、隐私政策、投诉流程、平台介绍、操作指南等。\n"
    "  → 这类问题不要调用任何工具，直接由知识库检索回答。\n"
    "\n"
    "类型B - 实时数据类（走工具）：\n"
    "  包括：查订单状态、查物流进度、查款式库存、查系列信息、查用户收藏、查抽盒记录。\n"
    "  → 这类问题必须调用对应工具获取实时数据。\n"
    "\n"
    "⚠️ 关键判断：如果用户问的不是具体的系列/商品名称，而是政策/规则/操作流程，\n"
    "  绝对不要调用 query_series_info 或 query_series_styles！\n"
    "  例如：「账号注销」「怎么退货」「支付方式」→ 走知识库，不调用工具。\n"
    "  例如：「LABUBU温暖系列」「动物宇宙」→ 走工具 query_series_info。\n"
    "\n"
    "【知识库与来源标注】\n"
    "你已关联知识库，优先从知识库检索回答。找到答案时直接使用文档原句回答，禁止改写。\n"
    "若知识库未检索到相关内容，用自己的知识回答，并说明该信息不在知识库中。\n"
    "\n"
    "【可用工具】你可以调用以下工具获取实时数据，禁止凭空编造订单号、物流单号、价格、库存：\n"
    "1. query_logistics(order_id) —— 查订单物流（用户问\"订单到哪了/快递进度\"时调用，需要具体订单号）\n"
    "2. query_series_info(series_name) —— 查系列摘要（仅当用户询问具体的潮玩系列名称时调用，如\"LABUBU温暖系列\"、\"动物宇宙系列\"，传入系列名称）\n"
    "3. query_series_styles(series_name) —— 查系列款式列表（仅当用户询问某个具体系列有哪些款式时调用）\n"
    "4. query_user_orders(user_id) —— 查用户订单列表（用户问\"我的订单/我买了什么\"时调用）\n"
    "5. query_user_favorites(user_id) —— 查用户收藏列表（用户问\"我的收藏\"时调用）\n"
    "6. query_user_draw_history(user_id) —— 查用户抽盒历史（用户问\"我抽过哪些\"时调用）\n"
    "7. query_style_stock(style_name) —— 查款式库存（用户问\"还有货吗\"时调用，需要具体款式名）\n"
    "\n"
    "【工具使用规则】\n"
    "- 需要实时数据时必须调用工具，不要用历史记忆回答订单/物流/库存等动态信息。\n"
    "- 涉及用户私有数据（订单/收藏/抽盒历史）的工具，请使用下方提供的当前用户ID。\n"
    "- 工具返回的数据可能为列表，请提炼要点后用简洁中文回复用户。\n"
    "- 只有用户明确提到具体的潮玩系列名/IP名时，才调用 query_series_info。\n"
    "\n"
    "【通用约束】\n"
    "1. 只回答与本平台业务相关的问题，对无关话题礼貌拒绝。\n"
    "2. 回复简洁，控制在200字以内，多用要点列表。\n"
    "3. 涉及具体退款/售后进度时，引导用户在App「订单详情-申请售后」操作。\n"
    "4. 若用户提供的信息不足（如缺少订单号、系列名），礼貌追问。"
)


def _save_message(user_id: str, session_id: str, role: str, content: str) -> dict[str, Any]:
    """保存消息到chat_message表"""
    msg_id = uuid.uuid4().hex
    now = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    execute(
        "INSERT INTO chat_message (message_id, user_id, session_id, role, content, create_time) "
        "VALUES (%s, %s, %s, %s, %s, %s)",
        (msg_id, user_id, session_id, role, content, now),
    )
    return {
        "messageId": msg_id,
        "userId": user_id,
        "sessionId": session_id,
        "role": role,
        "content": content,
        "createTime": now,
    }


def _load_history(user_id: str, session_id: str, limit: int = 10) -> list[dict[str, str]]:
    """加载最近若干轮历史对话，供大模型上下文使用"""
    rows = query_all(
        "SELECT role, content FROM chat_message "
        "WHERE user_id = %s AND session_id = %s "
        "ORDER BY create_time DESC LIMIT %s",
        (user_id, session_id, limit),
    )
    rows.reverse()
    return [{"role": r["role"], "content": r["content"]} for r in rows]


def _is_likely_non_series_query(message: str, tool_name: str, args: dict) -> bool:
    """
    校验工具调用是否合理：防止将非系列查询误路由到 series_info / series_styles
    例如：用户问"账号注销"，模型误调用 query_series_info(series_name="账号注销")
    """
    if tool_name not in _QUERY_TOOLS_REQUIRING_VALIDATION:
        return False

    # 获取传入的 series_name 参数
    series_name = (args.get("series_name") or "").strip()
    user_msg = message.strip()

    # 检查：用户消息中是否包含非系列查询关键词
    for kw in _NON_SERIES_KEYWORDS:
        if kw in user_msg:
            logger.warning(
                "工具调用校验：用户消息含非系列关键词「%s」，拦截 %s(series_name=%s)",
                kw, tool_name, series_name,
            )
            return True

    # 检查：series_name 本身是否像非系列查询
    for kw in _NON_SERIES_KEYWORDS:
        if kw in series_name:
            logger.warning(
                "工具调用校验：series_name 含非系列关键词「%s」，拦截 %s",
                kw, tool_name,
            )
            return True

    return False


def _add_source_tag(content: str, used_knowledge_base: bool) -> str:
    """
    代码层自动添加来源标识（不依赖 AI 自己标注）
    - used_knowledge_base=True：AI 回复中有 retrieval 工具调用 → 📌知识库
    - used_knowledge_base=False：AI 自身知识 → 💡AI知识
    先清除 AI 可能错误添加的标签，再统一添加正确的标签
    """
    if not content:
        return content

    # 清除 AI 可能错误添加的旧标签
    content = content.replace("📌知识库", "").strip()
    content = content.replace("💡AI知识", "").strip()

    if used_knowledge_base:
        return f"📌知识库 {content}"
    else:
        return f"💡AI知识 {content}"


def chat(user_id: str, session_id: str, message: str) -> dict[str, Any]:
    """
    主入口：处理用户消息并返回AI回复
    返回字段与原Java接口对齐：{ messageId, userId, sessionId, role, content, createTime }

    流程：
      1. 保存用户消息
      2. 本地FAQ优先匹配（静态知识，省Token）
      3. 问候语本地处理
      4. Function Call：实时数据走工具函数，最终由大模型汇总回复
    """
    cfg = get_settings()
    if not user_id:
        user_id = "anonymous"
    if not session_id:
        session_id = uuid.uuid4().hex
    if not message:
        message = ""

    # 1. 保存用户消息
    _save_message(user_id, session_id, "user", message)

    # 2. 本地FAQ优先匹配（省Token，速度快）
    faq_item, score = match_faq(message, cfg.FAQ_CONFIDENCE_THRESHOLD)
    if faq_item is not None:
        reply = faq_item.answer
        # FAQ也属于知识库来源，添加📌知识库标签
        reply = _add_source_tag(reply, True)
        logger.info("FAQ命中 userId=%s score=%.3f", user_id, score)
        return _save_message(user_id, session_id, "assistant", reply)

    # 3. 问候语直接本地处理
    if message and any(g in message for g in ["你好", "您好", "hi", "hello", "嗨", "在吗", "在不在"]):
        reply = (
            "您好！欢迎来到潮玩星球！我是AI智能客服，可以帮您解答订单、发货、退款、盲盒等问题。"
            "请问有什么可以帮助您的？"
        )
        return _save_message(user_id, session_id, "assistant", reply)

    # 4. 走 Function Call + ChatGLM
    reply, cards = _run_function_call_chat(user_id, session_id, message)

    msg = _save_message(user_id, session_id, "assistant", reply)
    # 附加结构化卡片（如系列卡片），供前端渲染可点击的卡片，前端负责跳转到 /series/{seriesId}
    if cards:
        msg["cards"] = cards
    return msg


def _route_question(message: str) -> str:
    """
    根据用户问题关键词，判断应该走知识库检索还是函数工具调用。

    ⚠️ 智谱API限制：函数调用与知识库检索互斥，同一次请求只能生效一个。
    优先级：函数调用 > 知识库检索 > 网络搜索。
    因此必须在发送请求前就确定使用哪种工具，不能同时放入。

    返回值：
      - "knowledge_base": 优先走知识库检索
      - "function_tools": 优先走函数工具
      - "kb_first": 优先知识库，如无结果再走函数工具（两阶段）
    """
    if not message:
        return "kb_first"

    msg = message.lower()

    # 计算知识库关键词匹配分
    kb_score = sum(1 for kw in _KB_PRIORITY_KEYWORDS if kw in msg)
    # 计算实时数据关键词匹配分
    rt_score = sum(1 for kw in _REALTIME_PRIORITY_KEYWORDS if kw in msg)

    if kb_score > rt_score:
        return "knowledge_base"
    elif rt_score > kb_score:
        return "function_tools"
    else:
        # 默认策略：先试知识库，再走函数工具
        return "kb_first"


def _try_knowledge_base(user_id: str, session_id: str, message: str) -> tuple[str, bool]:
    """
    阶段1：仅使用知识库检索（不传函数工具，避免互斥问题）

    返回: (回复文本, 是否命中知识库)
    """
    cfg = get_settings()

    # 组装 system prompt（知识库阶段不需要注入用户ID和函数工具说明）
    kb_system = (
        "你是「潮玩星球」电商平台的AI智能客服助手。\n"
        "你的职责：友好、专业、简洁地解答用户关于平台政策、规则、操作流程等问题。\n"
        "优先从知识库检索回答，找到答案时直接使用文档原句回答，禁止改写。\n"
        "若知识库未检索到相关内容，用自己的知识回答，并说明该信息不在知识库中。\n"
        "回复简洁，控制在200字以内，多用要点列表。"
    )
    messages: list[dict[str, Any]] = [{"role": "system", "content": kb_system}]
    messages.append({"role": "user", "content": message})

    try:
        result = chat_with_tools(messages, tools=None, use_knowledge_base=True)
    except Exception as e:
        logger.exception("知识库检索调用失败")
        return "", False

    content = result.get("content")
    if not content:
        return "", False

    # 判断是否命中知识库：如果回复中包含"不在知识库中"、"暂未检索到"等表述，说明未命中
    no_kb_phrases = [
        "不在知识库", "不在文档", "暂未检索到", "未检索到",
        "未找到相关", "没有找到", "未找到答案", "知识库中没有",
        "知识库中暂未",
    ]
    no_kb_match = any(phrase in content for phrase in no_kb_phrases)
    used_kb = not no_kb_match

    if used_kb:
        logger.info("【知识库】✅ 知识库命中，回复前100字: %s", content[:100])
    else:
        logger.info("【知识库】❌ 知识库未命中，回复前100字: %s", content[:100])

    return content, used_kb


def _run_function_call_chat(user_id: str, session_id: str, message: str) -> tuple[str, list[dict[str, Any]]]:
    """
    主入口：处理用户消息并返回AI回复
    返回字段与原Java接口对齐：{ messageId, userId, sessionId, role, content, createTime }

    流程（两阶段调用）：
      1. 本地FAQ优先匹配（静态知识，省Token）
      2. 问候语本地处理
      3. 知识库检索阶段：仅使用 retrieval 工具（智谱API互斥限制）
      4. 函数工具阶段：仅使用 function 工具（智谱API互斥限制）

    ⚠️ 智谱API限制：函数调用、知识库检索、网络搜索三者互斥！
      优先级：函数调用 > 知识库检索 > 网络搜索
      如果同时传入 retrieval 和 function 工具，知识库检索会被忽略！
      因此必须分两次调用，不能将两者放在同一个请求中。
    """
    cfg = get_settings()
    collected_cards: list[dict[str, Any]] = []

    # 根据问题类型决定调用策略
    route = _route_question(message)
    logger.info("路由决策: route=%s message=%s", route, message[:50])

    # === 阶段1：知识库检索（仅 retrieval 工具） ===
    if route in ("knowledge_base", "kb_first"):
        kb_reply, used_kb = _try_knowledge_base(user_id, session_id, message)
        if used_kb and kb_reply:
            # 知识库命中，直接返回
            kb_reply = _add_source_tag(kb_reply, True)
            return kb_reply, collected_cards
        # 知识库未命中：即使是"knowledge_base"路由，也继续尝试函数工具阶段
        # 因为路由判断可能不精确，用户的问题可能既需要知识库又需要函数工具

    # === 阶段2：函数工具调用（仅 function 工具） ===
    used_knowledge_base = False  # 追踪是否来自知识库

    # 组装 system prompt，注入当前用户ID（供 query_user_orders 等工具使用）
    system_content = f"{SYSTEM_PROMPT}\n\n【当前用户ID】{user_id}\n（调用查询用户私有数据的工具时，请传入此 user_id）"
    messages: list[dict[str, Any]] = [{"role": "system", "content": system_content}]

    # 拼接最近对话历史（仅保留 role/content 作为上下文，最多8轮）
    history = _load_history(user_id, session_id, limit=16)
    messages.extend(history)
    # 当前用户消息已保存入库，会被 _load_history 取到，避免重复追加
    if not (messages and messages[-1].get("role") == "user" and messages[-1].get("content") == message):
        messages.append({"role": "user", "content": message})

    max_iter = max(1, cfg.TOOL_CALL_MAX_ITERATIONS)
    for i in range(max_iter):
        try:
            result = chat_with_tools(messages, tools=TOOL_SCHEMAS, use_knowledge_base=False)
        except Exception as e:
            logger.exception("Function Call 调用失败")
            return f"{DEFAULT_REPLY}\n\n（AI服务暂时不可用：{type(e).__name__}）", collected_cards

        tool_calls = result.get("tool_calls")
        content = result.get("content")

        # 模型未请求工具，直接返回文本
        if not tool_calls:
            final_content = content or DEFAULT_REPLY
            final_content = _add_source_tag(final_content, used_knowledge_base)
            return final_content, collected_cards

        # 把带 tool_calls 的 assistant 消息加入上下文
        assistant_msg: dict[str, Any] = {
            "role": "assistant",
            "content": content or "",
            "tool_calls": tool_calls,
        }
        messages.append(assistant_msg)

        # 依次执行每个工具，把结果作为 tool 消息回传
        for tc in tool_calls:
            tc_id = tc.get("id")
            tc_type = tc.get("type", "")

            # retrieval 类型工具不应该出现在函数工具阶段，跳过
            if tc_type == "retrieval":
                logger.warning("函数工具阶段不应出现 retrieval 工具调用，跳过")
                messages.append({
                    "role": "tool",
                    "tool_call_id": tc_id,
                    "content": json.dumps({"info": "知识库检索已在上一阶段完成"}, ensure_ascii=False),
                })
                continue

            fn = tc.get("function") or {}
            name = fn.get("name")
            args = parse_tool_arguments(fn.get("arguments"))
            # 安全约束：用户私有数据查询强制使用当前登录用户的 ID
            if name in _USER_SPECIFIC_TOOLS:
                args["user_id"] = user_id

            # 校验工具调用合理性：防止非系列查询被误路由到 series_info / series_styles
            if _is_likely_non_series_query(message, name, args):
                # 返回空结果，让模型自己生成回答
                messages.append({
                    "role": "tool",
                    "tool_call_id": tc_id,
                    "content": json.dumps(
                        {"error": "该问题不适用于此工具，请直接使用自身知识回答"},
                        ensure_ascii=False,
                    ),
                })
                continue

            logger.info("执行工具 name=%s args=%s", name, args)
            tool_result = execute_tool(name, args)
            messages.append({
                "role": "tool",
                "tool_call_id": tc_id,
                "content": json.dumps(tool_result, ensure_ascii=False, default=str),
            })
            # 收集卡片：query_series_info 等工具的结果转为前端可渲染的卡片
            cards = build_cards_from_tool_result(name, tool_result)
            if cards:
                collected_cards.extend(cards)

    # 达到最大迭代仍未结束，做一次无工具调用以强制生成文本回复
    logger.warning("Function Call 达到最大迭代 %s，强制生成最终回复", max_iter)
    try:
        final = chat_with_tools(messages, tools=None, use_knowledge_base=False)
        final_content = final.get("content") or DEFAULT_REPLY
        final_content = _add_source_tag(final_content, used_knowledge_base)
        return final_content, collected_cards
    except Exception as e:
        logger.exception("最终回复生成失败")
        return f"{DEFAULT_REPLY}\n\n（AI服务暂时不可用：{type(e).__name__}）", collected_cards


def get_history(user_id: str, session_id: str, limit: int = 100) -> list[dict[str, Any]]:
    rows = query_all(
        "SELECT message_id, user_id, session_id, role, content, create_time "
        "FROM chat_message WHERE user_id = %s AND session_id = %s "
        "ORDER BY create_time ASC LIMIT %s",
        (user_id, session_id, limit),
    )
    out = []
    for r in rows:
        out.append({
            "messageId": r["message_id"],
            "userId": r["user_id"],
            "sessionId": r["session_id"],
            "role": r["role"],
            "content": r["content"],
            "createTime": r["create_time"].strftime("%Y-%m-%d %H:%M:%S") if hasattr(r["create_time"], "strftime") else r["create_time"],
        })
    return out


def get_sessions(user_id: str) -> list[str]:
    rows = query_all(
        "SELECT DISTINCT session_id FROM chat_message WHERE user_id = %s ORDER BY create_time DESC",
        (user_id,),
    )
    return [r["session_id"] for r in rows]
