"""
AI服务 FastAPI 入口
启动: uvicorn app:app --host 0.0.0.0 --port 8089 --reload
或:   python app.py

接口:
  POST /ai/customer-service/chat          智能客服对话
  GET  /ai/customer-service/history       获取会话历史
  GET  /ai/customer-service/sessions      获取会话列表
  POST /ai/recommend/personalized         个性化推荐
  GET  /ai/recommend/similar/{seriesId}   相似系列推荐
  GET  /ai/recommend/hot                  热门推荐
  POST /ai/behavior                       上报用户行为(供推荐算法使用)
  GET  /health                            健康检查
"""
import logging
from typing import Any

from fastapi import FastAPI, HTTPException, Query, Request
from fastapi.responses import JSONResponse
from pydantic import BaseModel
from starlette.middleware.base import BaseHTTPMiddleware

from config import get_settings
from customer_service import chat as cs_chat, get_history, get_sessions
from recommender import hot_recommend, record_behavior, recommend_for_user, recommend_similar

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
)
logger = logging.getLogger("ai-service")

app = FastAPI(title="Trendy-Toy AI Service", version="1.0.0")


# ============= 调试中间件：记录422请求体 =============
class Debug422Middleware(BaseHTTPMiddleware):
    async def dispatch(self, request: Request, call_next):
        body = b""
        if request.method == "POST":
            body = await request.body()
            # 把body重新塞回去，让后续解析能拿到
            async def receive():
                return {"type": "http.request", "body": body, "more_body": False}
            request._receive = receive
        response = await call_next(request)
        if response.status_code == 422:
            try:
                resp_body = b""
                async for chunk in response.body_iterator:
                    resp_body += chunk
                logger.warning(
                    "422 Debug | path=%s | method=%s | request_body=%s | response_body=%s",
                    request.url.path,
                    request.method,
                    body.decode("utf-8", errors="replace"),
                    resp_body.decode("utf-8", errors="replace"),
                )
                return JSONResponse(
                    status_code=422,
                    content={"detail": "see server log", "request_body": body.decode("utf-8", errors="replace")},
                    media_type="application/json",
                )
            except Exception as e:
                logger.error("中间件异常: %s", e)
        return response


app.add_middleware(Debug422Middleware)


# ============= 请求模型 =============
from pydantic import field_validator


class ChatRequest(BaseModel):
    userId: str | None = None
    message: str | None = None
    sessionId: str | None = None

    @field_validator("userId", "message", mode="before")
    @classmethod
    def _empty_to_str(cls, v):
        # null/None 统一转成空字符串，避免后续处理空指针
        if v is None:
            return ""
        return v

    model_config = {"extra": "ignore"}


class RecommendRequest(BaseModel):
    userId: str
    limit: int | None = 10
    seriesIds: list[str] | None = None


class BehaviorRequest(BaseModel):
    userId: str
    behaviorType: str
    targetType: str
    targetId: str
    weight: int = 1


# ============= 智能客服 =============
@app.post("/ai/customer-service/chat")
def customer_service_chat(req: ChatRequest):
    try:
        result = cs_chat(req.userId, req.sessionId or "", req.message)
        return {"code": 200, "message": "success", "data": result}
    except Exception as e:
        logger.exception("客服对话失败")
        raise HTTPException(status_code=500, detail=f"客服服务异常: {e}")


@app.get("/ai/customer-service/history")
def customer_service_history(
    userId: str = Query(...),
    sessionId: str = Query(...),
):
    try:
        rows = get_history(userId, sessionId)
        return {"code": 200, "message": "success", "data": rows}
    except Exception as e:
        logger.exception("获取历史失败")
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/ai/customer-service/sessions")
def customer_service_sessions(userId: str = Query(...)):
    try:
        sids = get_sessions(userId)
        return {"code": 200, "message": "success", "data": sids}
    except Exception as e:
        logger.exception("获取会话列表失败")
        raise HTTPException(status_code=500, detail=str(e))


# ============= 智能推荐 =============
@app.post("/ai/recommend/personalized")
def recommend_personalized(req: RecommendRequest):
    try:
        limit = req.limit or 10
        result = recommend_for_user(req.userId, limit)
        return {"code": 200, "message": "success", "data": result}
    except Exception as e:
        logger.exception("个性化推荐失败")
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/ai/recommend/similar/{series_id}")
def recommend_similar_api(series_id: str, limit: int = Query(6, ge=1, le=50)):
    try:
        result = recommend_similar(series_id, limit)
        return {"code": 200, "message": "success", "data": result}
    except Exception as e:
        logger.exception("相似推荐失败")
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/ai/recommend/hot")
def recommend_hot(limit: int = Query(10, ge=1, le=100)):
    try:
        result = hot_recommend(limit)
        return {"code": 200, "message": "success", "data": result}
    except Exception as e:
        logger.exception("热门推荐失败")
        raise HTTPException(status_code=500, detail=str(e))


# ============= 用户行为上报 =============
@app.post("/ai/behavior")
def report_behavior(req: BehaviorRequest):
    try:
        record_behavior(req.userId, req.behaviorType, req.targetType, req.targetId, req.weight)
        return {"code": 200, "message": "success", "data": None}
    except Exception as e:
        logger.exception("行为上报失败")
        raise HTTPException(status_code=500, detail=str(e))


# ============= 健康检查 =============
@app.get("/health")
def health():
    cfg = get_settings()
    return {
        "status": "UP",
        "service": "ai-service",
        "model": cfg.ZHIPU_MODEL,
        "db": cfg.DB_NAME,
        "apiKeyConfigured": bool(cfg.ZHIPU_API_KEY),
    }


if __name__ == "__main__":
    import uvicorn

    cfg = get_settings()
    uvicorn.run("app:app", host="0.0.0.0", port=cfg.AI_SERVICE_PORT, reload=True)
