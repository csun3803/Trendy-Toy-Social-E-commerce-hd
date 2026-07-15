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

SYSTEM_PROMPT = (
    "你是「潮玩星球」电商平台的AI智能客服助手。\n"
    "你的职责：友好、专业、简洁地解答用户关于订单、商品、盲盒、发货、退款、售后等问题。\n"
    "\n"
    "【可用工具】你可以调用以下工具获取实时数据，禁止凭空编造订单号、物流单号、价格、库存：\n"
    "1. query_logistics(order_id) —— 查订单物流（用户问“订单到哪了/快递进度”时调用）\n"
    "2. query_series_info(series_name) —— 查系列摘要（用户问“XX系列/XX款式/XXIP”时调用，返回系列ID/名称/款式数，前端会渲染为可点击卡片）\n"
    "3. query_series_styles(series_name) —— 查系列款式列表（用户问“XX系列有哪些款式”时调用）\n"
    "4. query_user_orders(user_id) —— 查用户订单列表（用户问“我的订单/我买了什么”时调用）\n"
    "5. query_user_favorites(user_id) —— 查用户收藏列表（用户问“我的收藏”时调用）\n"
    "6. query_user_draw_history(user_id) —— 查用户抽盒历史（用户问“我抽过哪些”时调用）\n"
    "7. query_style_stock(style_name) —— 查款式库存（用户问“还有货吗”时调用）\n"
    "\n"
    "【工具使用规则】\n"
    "- 需要实时数据时必须调用工具，不要用历史记忆回答订单/物流/库存等动态信息。\n"
    "- 涉及用户私有数据（订单/收藏/抽盒历史）的工具，请使用下方提供的当前用户ID。\n"
    "- 工具返回的数据可能为列表，请提炼要点后用简洁中文回复用户。\n"
    "- 当用户询问某个系列/IP/款式时，优先调用 query_series_info 获取系列信息，不要自己拼链接，前端会自动渲染卡片。\n"
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


def _run_function_call_chat(user_id: str, session_id: str, message: str) -> tuple[str, list[dict[str, Any]]]:
    """
    执行 Function Call 对话循环：
      - 调用大模型（携带工具 schema）
      - 若模型返回 tool_calls，执行对应工具（实时数据来自 Java 接口），把结果回传给模型
      - 循环直到模型给出最终文本回复，或达到最大迭代轮数
    返回: (回复文本, 卡片列表)
      - 卡片列表来自 query_series_info 等工具的返回结果，供前端渲染可点击卡片
    """
    cfg = get_settings()
    collected_cards: list[dict[str, Any]] = []

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
            result = chat_with_tools(messages, tools=TOOL_SCHEMAS)
        except Exception as e:
            logger.exception("Function Call 调用失败")
            return f"{DEFAULT_REPLY}\n\n（AI服务暂时不可用：{type(e).__name__}）", collected_cards

        tool_calls = result.get("tool_calls")
        content = result.get("content")

        # 模型未请求工具，直接返回文本
        if not tool_calls:
            return content or DEFAULT_REPLY, collected_cards

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
            fn = tc.get("function") or {}
            name = fn.get("name")
            args = parse_tool_arguments(fn.get("arguments"))
            # 安全约束：用户私有数据查询强制使用当前登录用户的 ID
            if name in _USER_SPECIFIC_TOOLS:
                args["user_id"] = user_id
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
        final = chat_with_tools(messages, tools=None)
        return final.get("content") or DEFAULT_REPLY, collected_cards
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
