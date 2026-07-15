"""
数据库连接管理（连接池 + 简单查询封装）
"""
from typing import Any, Optional
from dbutils.pooled_db import PooledDB
import pymysql

from config import get_settings

_pool: Optional[PooledDB] = None


def _get_pool() -> PooledDB:
    global _pool
    if _pool is None:
        cfg = get_settings()
        _pool = PooledDB(
            creator=pymysql,
            mincached=2,
            maxcached=10,
            maxconnections=20,
            blocking=True,
            cursorclass=pymysql.cursors.DictCursor,
            **cfg.db_dsn,
        )
    return _pool


def query_all(sql: str, args: tuple | list | None = None) -> list[dict[str, Any]]:
    """查询多行"""
    pool = _get_pool()
    conn = pool.connection()
    try:
        with conn.cursor() as cur:
            cur.execute(sql, args or ())
            rows = cur.fetchall()
            return list(rows)
    finally:
        conn.close()


def query_one(sql: str, args: tuple | list | None = None) -> Optional[dict[str, Any]]:
    """查询单行"""
    rows = query_all(sql, args)
    return rows[0] if rows else None


def execute(sql: str, args: tuple | list | None = None) -> int:
    """执行写操作，返回受影响行数"""
    pool = _get_pool()
    conn = pool.connection()
    try:
        with conn.cursor() as cur:
            rows = cur.execute(sql, args or ())
            conn.commit()
            return rows
    except Exception:
        conn.rollback()
        raise
    finally:
        conn.close()
