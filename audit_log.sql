-- 审计日志表
CREATE TABLE IF NOT EXISTS `audit_log` (
  `log_id` VARCHAR(36) NOT NULL COMMENT '日志ID',
  `operator_id` VARCHAR(36) DEFAULT NULL COMMENT '操作人ID',
  `operator_name` VARCHAR(100) DEFAULT NULL COMMENT '操作人用户名/工号',
  `operator_type` VARCHAR(20) DEFAULT NULL COMMENT '操作人类型: PLATFORM_ADMIN/SHOP_ADMIN',
  `action` VARCHAR(20) DEFAULT NULL COMMENT '操作类型: CREATE/UPDATE/DELETE/LOGIN/APPROVE/REJECT',
  `module` VARCHAR(20) DEFAULT NULL COMMENT '操作模块: USER/SHOP/ORDER/ADMIN/ACTIVITY/ALBUM',
  `description` VARCHAR(255) DEFAULT NULL COMMENT '操作描述',
  `target_id` VARCHAR(36) DEFAULT NULL COMMENT '操作目标ID',
  `target_type` VARCHAR(50) DEFAULT NULL COMMENT '操作目标类型',
  `method` VARCHAR(10) DEFAULT NULL COMMENT '请求方法: GET/POST/PUT/DELETE',
  `request_url` VARCHAR(255) DEFAULT NULL COMMENT '请求路径',
  `request_params` TEXT DEFAULT NULL COMMENT '请求参数',
  `response_code` INT DEFAULT NULL COMMENT '响应状态码',
  `ip_address` VARCHAR(50) DEFAULT NULL COMMENT '操作者IP',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`log_id`),
  KEY `idx_operator_id` (`operator_id`),
  KEY `idx_module` (`module`),
  KEY `idx_action` (`action`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审计日志表';
