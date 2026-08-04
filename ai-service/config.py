"""
全局配置加载
从环境变量 / .env 文件读取配置
"""
from functools import lru_cache
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")

    # 数据库
    DB_HOST: str = "localhost"
    DB_PORT: int = 3306
    DB_NAME: str = "chaowan_platform"
    DB_USER: str = "root"
    DB_PASSWORD: str = "sql2008"

    # 智谱 ChatGLM
    ZHIPU_API_KEY: str = ""
    ZHIPU_MODEL: str = "glm-4"  # 可改为 glm-4 以获得更稳定的响应
    ZHIPU_TIMEOUT: int = 60  # 增加到 60 秒，防止知识库检索耗时过长
    ZHIPU_KNOWLEDGE_BASE_ID: str = "2076583801819222016"  # 知识库ID
    FAQ_CONFIDENCE_THRESHOLD: float = 0.35

    # 服务
    AI_SERVICE_PORT: int = 8089
    FAQ_POLISH_WITH_LLM: bool = False
    RAG_RECENT_ORDER_LIMIT: int = 3
    RAG_PRODUCT_LIMIT: int = 5

    # Java 后端地址（供 Function Call 工具函数 HTTP 调用获取实时数据）
    JAVA_BACKEND_URL: str = "http://localhost:8080"
    # 工具函数调用 Java 接口的超时时间(秒)
    TOOLS_HTTP_TIMEOUT: int = 8
    # Function Call 最大迭代轮数（防止大模型反复调用工具陷入死循环）
    TOOL_CALL_MAX_ITERATIONS: int = 3

    @property
    def db_dsn(self) -> dict:
        return {
            "host": self.DB_HOST,
            "port": self.DB_PORT,
            "db": self.DB_NAME,
            "user": self.DB_USER,
            "password": self.DB_PASSWORD,
            "charset": "utf8mb4",
        }


@lru_cache
def get_settings() -> Settings:
    return Settings()
