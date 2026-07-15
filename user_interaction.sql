CREATE TABLE IF NOT EXISTS `user_interaction` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `interaction_id` VARCHAR(64) NOT NULL UNIQUE COMMENT '交互记录ID',
  `user_id` VARCHAR(64) NOT NULL COMMENT '操作用户ID',
  `target_type` VARCHAR(20) NOT NULL COMMENT '目标类型: ACTIVITY/COMMENT/PRODUCT/USER',
  `target_id` VARCHAR(64) NOT NULL COMMENT '目标ID',
  `action_type` VARCHAR(20) NOT NULL COMMENT '行为类型: LIKE/FAVORITE/FOLLOW/VIEW',
  `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/CANCELLED',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_user_target_action` (`user_id`, `target_type`, `target_id`, `action_type`),
  INDEX `idx_target` (`target_type`, `target_id`, `action_type`, `status`),
  INDEX `idx_user` (`user_id`, `action_type`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户交互记录表';
