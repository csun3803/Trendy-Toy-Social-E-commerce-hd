-- 社区动态审核流程（先发后审）数据库变更

-- 1. 创建举报表
CREATE TABLE IF NOT EXISTS `report` (
  `report_id` VARCHAR(36) NOT NULL COMMENT '举报ID',
  `reporter_id` VARCHAR(36) NOT NULL COMMENT '举报人ID',
  `target_type` VARCHAR(20) NOT NULL COMMENT '举报目标类型(ACTIVITY/COMMENT)',
  `target_id` VARCHAR(36) NOT NULL COMMENT '举报目标ID',
  `reason` VARCHAR(500) NOT NULL COMMENT '举报原因',
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态(PENDING/RESOLVED/DISMISSED)',
  `resolved_by` VARCHAR(36) DEFAULT NULL COMMENT '处理人ID',
  `resolved_at` DATETIME DEFAULT NULL COMMENT '处理时间',
  `resolve_notes` VARCHAR(500) DEFAULT NULL COMMENT '处理备注',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`report_id`),
  KEY `idx_target` (`target_type`, `target_id`),
  KEY `idx_reporter` (`reporter_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='举报表';

-- 2. social_activity 表增加字段
ALTER TABLE `social_activity`
  ADD COLUMN `report_count` INT NOT NULL DEFAULT 0 COMMENT '举报次数(缓存)',
  ADD COLUMN `has_pending_report` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否有待处理举报(0否1是)';
