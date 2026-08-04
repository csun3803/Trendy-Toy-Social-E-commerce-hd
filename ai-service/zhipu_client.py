"""
智谱AI ChatGLM API 客户端
文档: https://open.bigmodel.cn/dev/api
"""
import json
import logging
import time
from typing import Any

import httpx

from config import get_settings

logger = logging.getLogger("zhipu")

_ZHIPU_URL = "https://open.bigmodel.cn/api/paas/v4/chat/completions"

# 重试配置
MAX_RETRIES = 3  # 最大重试次数
RETRY_DELAY_BASE = 1  # 基础延迟时间（秒）
RATE_LIMIT_DELAY = 5  # 429 限流时的基础延迟时间（秒）


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
        "messages": messages,
    }
    # 注意：不传 max_tokens，让模型自行决定输出长度
    # GLM-4-flash 对 max_tokens 参数敏感，可能导致超时或无响应
    if extra:
        payload.update(extra)

    headers = {
        "Authorization": f"Bearer {cfg.ZHIPU_API_KEY}",
        "Content-Type": "application/json",
    }

    # 重试循环（处理 429、5xx 错误和超时）
    last_error = None
    for attempt in range(MAX_RETRIES):
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
            status_code = e.response.status_code
            last_error = e

            # 429 (Too Many Requests) 或 5xx 服务器错误，进行重试
            if status_code in (429, 500, 502, 503, 504):
                if attempt < MAX_RETRIES - 1:
                    # 429 限流使用更长的延迟
                    if status_code == 429:
                        delay = RATE_LIMIT_DELAY * (attempt + 1)  # 5s, 10s, 15s
                    else:
                        delay = RETRY_DELAY_BASE * (2 ** attempt)  # 指数退避: 1s, 2s, 4s
                    logger.warning(
                        "ChatGLM HTTP %d，第 %d 次重试，等待 %d 秒",
                        status_code, attempt + 1, delay
                    )
                    time.sleep(delay)
                    continue

            logger.error("ChatGLM HTTP错误: %s, 响应: %s", e, e.response.text[:500])
            return f"（AI服务暂时不可用，HTTP {status_code}）请稍后再试或换种问法。"

        except (httpx.ReadTimeout, httpx.ConnectTimeout) as e:
            # 超时错误，进行重试
            last_error = e
            if attempt < MAX_RETRIES - 1:
                delay = RETRY_DELAY_BASE * (2 ** attempt)
                logger.warning(
                    "ChatGLM 请求超时，第 %d 次重试，等待 %d 秒",
                    attempt + 1, delay
                )
                time.sleep(delay)
                continue
            logger.error("ChatGLM 请求超时，重试 %d 次后仍失败", MAX_RETRIES)
            return f"（AI服务响应超时，已重试 {MAX_RETRIES} 次）请稍后再试。"

        except Exception as e:
            logger.exception("ChatGLM调用异常")
            return f"（AI服务异常：{type(e).__name__}）请稍后再试。"

    # 所有重试都失败
    return f"（AI服务重试 {MAX_RETRIES} 次后仍失败）请稍后再试。"


def chat_with_tools(
    messages: list[dict[str, Any]],
    tools: list[dict[str, Any]] | None = None,
    temperature: float = 0.5,
    max_tokens: int = 1024,
    use_knowledge_base: bool = False,
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
    :param tools: 函数工具 schema 列表（OpenAI 兼容格式），仅 function 类型工具
    :param use_knowledge_base: 是否使用知识库检索（retrieval 工具）
        ⚠️ 智谱API限制：函数调用、知识库检索、网络搜索三者互斥，同一次请求只能生效一个。
        优先级：函数调用 > 知识库检索 > 网络搜索。
        因此不能将 retrieval 和 function 工具放在同一个 tools 数组中，必须分两次调用。
    """
    cfg = get_settings()
    if not cfg.ZHIPU_API_KEY:
        raise RuntimeError("未配置 ZHIPU_API_KEY，请先在 .env 中设置智谱API Key")

    payload: dict[str, Any] = {
        "model": cfg.ZHIPU_MODEL,
        "temperature": temperature,
        "messages": messages,
    }
    # 注意：不传 max_tokens，让模型自行决定输出长度
    # GLM-4-flash 对 max_tokens 参数敏感，可能导致超时或无响应

    # ⚠️ 关键：智谱API的函数调用与知识库检索互斥！
    # 优先级：函数调用 > 知识库检索 > 网络搜索
    # 如果同时传入 retrieval 和 function 工具，知识库检索会被忽略！
    # 解决方案：分两次调用——先知识库（use_knowledge_base=True），后函数工具（tools=FUNCTION_SCHEMAS）

    if use_knowledge_base and cfg.ZHIPU_KNOWLEDGE_BASE_ID:
        # 仅使用知识库检索工具（不传函数工具，避免互斥问题）
        retrieval_tool = {
            "type": "retrieval",
            "retrieval": {
                "knowledge_id": cfg.ZHIPU_KNOWLEDGE_BASE_ID,
                "prompt_template": (
                    "从文档\n\"\"\"\n{{knowledge}}\n\"\"\"\n中找问题\n\"\"\"\n{{question}}\n\"\"\"\n的答案。\n\n"
                    "【回答规则】\n"
                    "1. 找到答案时：直接逐字引用文档原句回答，禁止改写、禁止添加、禁止省略关键信息\n"
                    "2. 未找到答案时：用自己的知识回答，并说明\"该信息不在知识库中\"\n"
                    "3. 不要自己添加任何来源标注标签\n"
                    "4. 不要复述问题，直接开始回答。"
                )
            }
        }
        payload["tools"] = [retrieval_tool]
        payload["tool_choice"] = "auto"
    elif tools:
        # 仅使用函数工具（不传 retrieval，避免互斥问题）
        payload["tools"] = tools
        payload["tool_choice"] = "auto"

    headers = {
        "Authorization": f"Bearer {cfg.ZHIPU_API_KEY}",
        "Content-Type": "application/json",
    }

    # 重试循环（处理 429 和 5xx 错误）
    last_error = None
    for attempt in range(MAX_RETRIES):
        try:
            # 调试日志：记录请求参数（排除敏感信息）
            logger.debug(
                "ChatGLM(工具) 请求参数 model=%s tools_count=%d messages_count=%d temperature=%s",
                cfg.ZHIPU_MODEL,
                len(payload.get("tools", [])),
                len(messages),
                temperature,
            )
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

            # ===== 知识库调试日志 =====
            if cfg.ZHIPU_KNOWLEDGE_BASE_ID:
                # 检查是否有 retrieval 工具调用
                has_retrieval = False
                if tool_calls:
                    for tc in tool_calls:
                        if tc.get("type") == "retrieval":
                            has_retrieval = True
                            logger.info("【知识库】调用了 retrieval 工具，ID=%s", tc.get("id"))
                            break

                # 检查回复内容来源标识
                if content:
                    if "📌知识库" in content:
                        logger.info("【知识库】✅ 回答基于知识库")
                    elif "💡AI知识" in content:
                        logger.info("【知识库】❌ 回答来自AI自身知识")
                    else:
                        logger.info("【知识库】⚠️ 回答未包含来源标识，内容：%s", content[:100])

            return {
                "content": content,
                "tool_calls": tool_calls,
                "finish_reason": finish_reason,
                "raw": message,
            }

        except httpx.HTTPStatusError as e:
            status_code = e.response.status_code
            last_error = e

            # 429 (Too Many Requests) 或 5xx 服务器错误，进行重试
            if status_code in (429, 500, 502, 503, 504):
                if attempt < MAX_RETRIES - 1:
                    # 429 限流使用更长的延迟
                    if status_code == 429:
                        delay = RATE_LIMIT_DELAY * (attempt + 1)  # 5s, 10s, 15s
                    else:
                        delay = RETRY_DELAY_BASE * (2 ** attempt)  # 指数退避: 1s, 2s, 4s
                    logger.warning(
                        "ChatGLM(工具) HTTP %d，第 %d 次重试，等待 %d 秒",
                        status_code, attempt + 1, delay
                    )
                    time.sleep(delay)
                    continue

            logger.error("ChatGLM(工具) HTTP错误: %s, 响应: %s", e, e.response.text[:500])
            raise RuntimeError(f"ChatGLM HTTP {status_code}") from e

        except (httpx.ReadTimeout, httpx.ConnectTimeout) as e:
            # 超时错误，进行重试
            last_error = e
            if attempt < MAX_RETRIES - 1:
                delay = RETRY_DELAY_BASE * (2 ** attempt)
                logger.warning(
                    "ChatGLM(工具) 请求超时，第 %d 次重试，等待 %d 秒",
                    attempt + 1, delay
                )
                time.sleep(delay)
                continue
            logger.error("ChatGLM(工具) 请求超时，重试 %d 次后仍失败", MAX_RETRIES)
            raise RuntimeError(f"ChatGLM 请求超时，已重试 {MAX_RETRIES} 次") from e

        except Exception as e:
            logger.exception("ChatGLM(工具)调用异常")
            raise

    # 所有重试都失败
    if last_error:
        raise RuntimeError(f"ChatGLM 重试 {MAX_RETRIES} 次后仍失败") from last_error


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

