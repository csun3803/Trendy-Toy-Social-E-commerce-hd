-- 客服会话表升级：新增统一会话管理字段
-- AI模式30分钟超时，人工模式60分钟超时
-- ai_session_id 用于关联 AI 聊天的 chat_message 表

ALTER TABLE `customer_service_session`
  ADD COLUMN `mode` VARCHAR(10) DEFAULT 'AI' COMMENT '会话模式: AI/HUMAN' AFTER `status`,
  ADD COLUMN `last_active_time` DATETIME DEFAULT NULL COMMENT '最后活跃时间(用于超时判断)' AFTER `mode`,
  ADD COLUMN `ai_session_id` VARCHAR(64) DEFAULT NULL COMMENT 'AI聊天会话ID(关联chat_message表)' AFTER `last_active_time`,
  ADD INDEX `idx_mode` (`mode`),
  ADD INDEX `idx_last_active_time` (`last_active_time`);
