-- 系统通知表
CREATE TABLE IF NOT EXISTS `system_notification` (
  `notification_id` VARCHAR(64) NOT NULL COMMENT '通知ID',
  `user_id` VARCHAR(64) NOT NULL COMMENT '用户ID（通知接收者）',
  `title` VARCHAR(255) NOT NULL COMMENT '通知标题',
  `content` TEXT DEFAULT NULL COMMENT '通知内容摘要',
  `category` VARCHAR(32) NOT NULL DEFAULT 'SYSTEM' COMMENT '通知分类: COUPON优惠券/ORDER订单/INTERACTION互动/SYSTEM系统',
  `related_id` VARCHAR(64) DEFAULT NULL COMMENT '关联业务ID（如优惠券ID、订单ID等）',
  `related_type` VARCHAR(32) DEFAULT NULL COMMENT '关联业务类型（user_coupon/order等），用于跳转',
  `is_read` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已读: 0未读/1已读',
  `read_at` DATETIME DEFAULT NULL COMMENT '已读时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`notification_id`),
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_user_category` (`user_id`, `category`),
  INDEX `idx_user_read` (`user_id`, `is_read`),
  INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统通知表';
