# AI Service (潮玩星球智能服务)

独立 Python AI 模块，与 Spring Boot 后端 (`src/`) 解耦，提供：
1. **智能客服**：调用智谱 ChatGLM，FAQ 本地优先 + RAG 增强（订单/商品检索拼接到 Prompt）
2. **智能推荐**：内容召回 + 协同过滤 + 热度兜底的混合推荐算法

## 目录结构

```
ai-service/
├── app.py                # FastAPI 入口，对外接口
├── config.py             # 配置加载（.env）
├── db.py                 # MySQL 连接池 & 查询封装
├── faq_kb.py             # 本地 FAQ 知识库（关键词+Jaccard打分）
├── zhipu_client.py       # 智谱 ChatGLM API 客户端
├── rag.py                # RAG：检索订单/商品拼接到 Prompt
├── customer_service.py   # 客服编排：FAQ -> RAG -> ChatGLM
├── recommender.py        # 推荐算法：内容+协同+热度混合
├── requirements.txt
├── .env.example
└── README.md
```

## 快速开始

### 1. 安装依赖

```bash
cd ai-service
pip install -r requirements.txt
```

### 2. 配置

复制 `.env.example` 为 `.env`，填入：
- 数据库连接信息（与 Spring Boot 后端共用同一个 MySQL）
- `ZHIPU_API_KEY`：前往 https://open.bigmodel.cn/usercenter/apikeys 申请

### 3. 启动

```bash
python app.py
# 或
uvicorn app:app --host 0.0.0.0 --port 8089 --reload
```

默认监听 `8089`。健康检查：`GET http://localhost:8089/health`

## 接口说明

### 智能客服

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/ai/customer-service/chat` | 智能客服对话 |
| GET  | `/ai/customer-service/history?userId=&sessionId=` | 获取会话历史 |
| GET  | `/ai/customer-service/sessions?userId=` | 获取会话列表 |

**对话流程**（请求体与 Java 后端原接口完全一致，前端无感切换）：

```
用户消息
   ↓
本地FAQ知识库匹配(关键词+Jaccard)
   ├─ 命中(置信度≥0.35) → 直接返回本地答案（省Token, ~5ms）
   └─ 未命中
       ↓
   问候语识别 → 本地回复
       ↓
   RAG上下文构建(检测意图: 订单/商品/店铺)
       ↓
   拼接到System Prompt
       ↓
   调用智谱ChatGLM → 返回回复
```

### 智能推荐

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/ai/recommend/personalized` | 个性化推荐（已登录用户） |
| GET  | `/ai/recommend/similar/{seriesId}?limit=6` | 相似系列推荐 |
| GET  | `/ai/recommend/hot?limit=10` | 热门推荐（无需登录） |

**个性化推荐算法（混合推荐）**：

```
score = 0.45 * content_score      # 内容相似度(theme/ip/价位命中)
      + 0.30 * cf_score           # 协同过滤(相似用户也喜欢的)
      + 0.25 * popularity_score   # 全局热度
      - 0.85 * seen_penalty       # 已交互系列降权(不完全排除)

数据来源: user_interaction表(收藏/浏览商品) + 订单数据(购买)
冷启动: 用户无行为记录时, 自动回退到热度推荐
```

## 与 Java 后端集成

Java 后端 (`AiCustomerService` / `AiRecommendService`) 通过 HTTP 调用本服务。
在 `application.properties` 中配置：

```properties
ai.service.base-url=http://localhost:8089
```

前端 (React Native) 调用 `/api/ai/*` 接口路径**保持不变**，由 Java 后端转发到 Python 服务。

## 设计要点

1. **FAQ 优先**：常见问题（"怎么退换货"等）本地秒回，省 Token、低延迟
2. **RAG 增强**：检测到订单/商品相关问题时，先查 MySQL，把结构化数据拼到 Prompt，避免大模型幻觉订单号/物流单
3. **混合推荐**：内容+协同+热度三路召回融合打分，兼顾个性化与冷启动
4. **解耦设计**：独立 Python 进程，便于迭代算法而不重启 Java 服务
