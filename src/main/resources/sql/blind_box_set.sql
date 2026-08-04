-- ============================================================
-- 套盒系统：一个抽盒机有多套盒，每套盒有固定格子（3x3=9格）
-- ============================================================

-- 1. 套盒表
CREATE TABLE IF NOT EXISTS `blind_box_set` (
    `set_id` VARCHAR(64) NOT NULL COMMENT '套盒ID',
    `machine_id` VARCHAR(64) NOT NULL COMMENT '关联抽盒机ID',
    `set_index` INT NOT NULL DEFAULT 0 COMMENT '套盒序号（用于排序和左右切换）',
    `set_name` VARCHAR(100) DEFAULT NULL COMMENT '套盒名称（如：第1套）',
    `layout_image` VARCHAR(500) DEFAULT NULL COMMENT '盒位图URL（展示套盒格子布局的图片）',
    `grid_rows` INT NOT NULL DEFAULT 3 COMMENT '行数',
    `grid_cols` INT NOT NULL DEFAULT 3 COMMENT '列数',
    `total_slots` INT NOT NULL DEFAULT 9 COMMENT '总格数 = rows * cols',
    `sold_count` INT NOT NULL DEFAULT 0 COMMENT '已售格数',
    `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE活跃/COMPLETED已售完',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`set_id`),
    INDEX `idx_machine` (`machine_id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='抽盒机套盒表';

-- 2. 给 blind_box_slot 表添加 set_id 字段（关联套盒）
-- 如果 blind_box_slot 表已存在，执行以下 ALTER：
ALTER TABLE `blind_box_slot` ADD COLUMN IF NOT EXISTS `set_id` VARCHAR(64) DEFAULT NULL COMMENT '关联套盒ID' AFTER `machine_id`;
ALTER TABLE `blind_box_slot` ADD INDEX IF NOT EXISTS `idx_set_id` (`set_id`);

-- 3. 数据迁移：为现有 slot 创建默认套盒
-- INSERT INTO blind_box_set (set_id, machine_id, set_index, set_name, total_slots, sold_count, status)
-- SELECT CONCAT('set_', machine_id, '_0'), machine_id, 0, '第1套', 9, COUNT(CASE WHEN status='SOLD' THEN 1 END), 'ACTIVE'
-- FROM blind_box_slot GROUP BY machine_id;
-- UPDATE blind_box_slot SET set_id = CONCAT('set_', machine_id, '_0') WHERE set_id IS NULL;
