-- 盲盒槽位表（九宫格选盒）
CREATE TABLE IF NOT EXISTS `blind_box_slot` (
    `slot_id` VARCHAR(64) NOT NULL COMMENT '槽位ID',
    `machine_id` VARCHAR(64) NOT NULL COMMENT '关联的抽盒机ID',
    `slot_no` INT NOT NULL COMMENT '槽位编号(1-9)',
    `slot_code` VARCHAR(64) NOT NULL COMMENT '槽位编码',
    `status` VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE' COMMENT '状态: AVAILABLE/RESERVED/SOLD/SELECTED',
    `sale_variant_id` VARCHAR(64) DEFAULT NULL COMMENT '预分配的销售款式ID(选盒后揭晓)',
    `variant_id` VARCHAR(64) DEFAULT NULL COMMENT '预分配的原始产品ID',
    `is_hidden` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否为隐藏款(揭晓后填入)',
    `drawn_by` VARCHAR(64) DEFAULT NULL COMMENT '抽中者用户ID',
    `drawn_at` DATETIME DEFAULT NULL COMMENT '抽中时间',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`slot_id`),
    KEY `idx_machine_id` (`machine_id`),
    KEY `idx_status` (`status`),
    UNIQUE KEY `uk_machine_slot_no` (`machine_id`, `slot_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='盲盒槽位表(九宫格选盒)';

-- 盲盒排队表
CREATE TABLE IF NOT EXISTS `blind_box_queue` (
    `queue_id` VARCHAR(64) NOT NULL COMMENT '排队记录ID',
    `machine_id` VARCHAR(64) NOT NULL COMMENT '关联的抽盒机ID',
    `user_id` VARCHAR(64) NOT NULL COMMENT '用户ID',
    `queue_position` INT NOT NULL COMMENT '队列位置',
    `status` VARCHAR(20) NOT NULL DEFAULT 'WAITING' COMMENT '状态: WAITING/ACTIVE/LEFT',
    `joined_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    `left_at` DATETIME DEFAULT NULL COMMENT '离开时间',
    PRIMARY KEY (`queue_id`),
    KEY `idx_machine_id` (`machine_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='盲盒排队表';
