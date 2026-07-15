"""
智谱AI ChatGLM API 客户端
文档: https://open.bigmodel.cn/dev/api
"""
import json
import logging
from typing import Any

import httpx

from config import get_settings

logger = logging.getLogger("zhipu")

_ZHIPU_URL = "https://open.bigmodel.cn/api/paas/v4/chat/completions"


def chat(
    messages: list[dict[str, str]],
    temperature: float = 0.6,
    max_tokens: int = 1024,
    extra: dict[str, Any] | None = None,
) -> str:
    """
    调用智谱ChatGLM接口
    :param messages: [{"role":"system"|"user"|"assistant", "content":"..."}]
    :return: 模型回复文本
    """
    cfg = get_settings()
    if not cfg.ZHIPU_API_KEY:
        raise RuntimeError("未配置 ZHIPU_API_KEY，请先在 .env 中设置智谱API Key")

    payload: dict[str, Any] = {
        "model": cfg.ZHIPU_MODEL,
        "temperature": temperature,
        "max_tokens": max_tokens,
        "messages": messages,
    }
    if extra:
        payload.update(extra)

    headers = {
        "Authorization": f"Bearer {cfg.ZHIPU_API_KEY}",
        "Content-Type": "application/json",
    }

    try:
        with httpx.Client(timeout=cfg.ZHIPU_TIMEOUT) as client:
            resp = client.post(_ZHIPU_URL, json=payload, headers=headers)
            resp.raise_for_status()
            data = resp.json()
        # 标准OpenAI格式
        choices = data.get("choices") or []
        if not choices:
            logger.warning("ChatGLM返回空choices: %s", data)
            return "抱歉，我暂时无法生成回复，请稍后再试。"
        return choices[0]["message"]["content"].strip()
    except httpx.HTTPStatusError as e:
        logger.error("ChatGLM HTTP错误: %s, 响应: %s", e, e.response.text[:500])
        return f"（AI服务暂时不可用，HTTP {e.response.status_code}）请稍后再试或换种问法。"
    except Exception as e:
        logger.exception("ChatGLM调用异常")
        return f"（AI服务异常：{type(e).__name__}）请稍后再试。"


def chat_with_tools(
    messages: list[dict[str, Any]],
    tools: list[dict[str, Any]] | None = None,
    temperature: float = 0.5,
    max_tokens: int = 1024,
) -> dict[str, Any]:
    """
    调用智谱ChatGLM接口（支持 Function Call / 工具调用）
    返回结构:
        {
            "content": str | None,        # 模型回复文本（可能为空）
            "tool_calls": list | None,    # 工具调用列表，每个元素含 id/function{name,arguments}
            "finish_reason": str,         # stop / tool_calls
            "raw": dict                   # 原始 message（调试用）
        }
    :param messages: 完整消息列表（含 tool 消息时需保留 tool_calls 与 tool_call_id）
    :param tools: 工具 schema 列表（OpenAI 兼容格式）
    """
    cfg = get_settings()
    if not cfg.ZHIPU_API_KEY:
        raise RuntimeError("未配置 ZHIPU_API_KEY，请先在 .env 中设置智谱API Key")

    payload: dict[str, Any] = {
        "model": cfg.ZHIPU_MODEL,
        "temperature": temperature,
        "max_tokens": max_tokens,
        "messages": messages,
    }
    if tools:
        payload["tools"] = tools
        payload["tool_choice"] = "auto"

    headers = {
        "Authorization": f"Bearer {cfg.ZHIPU_API_KEY}",
        "Content-Type": "application/json",
    }

    try:
        with httpx.Client(timeout=cfg.ZHIPU_TIMEOUT) as client:
            resp = client.post(_ZHIPU_URL, json=payload, headers=headers)
            resp.raise_for_status()
            data = resp.json()
        choices = data.get("choices") or []
        if not choices:
            logger.warning("ChatGLM(工具)返回空choices: %s", data)
            return {"content": None, "tool_calls": None, "finish_reason": "empty", "raw": {}}
        message = choices[0].get("message") or {}
        finish_reason = choices[0].get("finish_reason")
        # content 可能为 None（模型只调用工具不输出文本）
        content = message.get("content")
        if isinstance(content, str):
            content = content.strip() or None
        tool_calls = message.get("tool_calls")
        return {
            "content": content,
            "tool_calls": tool_calls,
            "finish_reason": finish_reason,
            "raw": message,
        }
    except httpx.HTTPStatusError as e:
        logger.error("ChatGLM(工具) HTTP错误: %s, 响应: %s", e, e.response.text[:500])
        raise RuntimeError(f"ChatGLM HTTP {e.response.status_code}") from e
    except Exception as e:
        logger.exception("ChatGLM(工具)调用异常")
        raise


def parse_tool_arguments(arguments: Any) -> dict[str, Any]:
    """解析工具调用参数（可能是字符串JSON或已解析字典）"""
    if arguments is None:
        return {}
    if isinstance(arguments, dict):
        return arguments
    if isinstance(arguments, str):
        try:
            return json.loads(arguments)
        except json.JSONDecodeError as e:
            logger.warning("工具参数JSON解析失败: %s err=%s", arguments, e)
            return {}
    return {}

