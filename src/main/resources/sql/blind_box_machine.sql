-- ============================================================
-- 抽盒机管理模块数据库脚本（v2 - 商家端 + 管理员端监管）
-- ============================================================

-- 1. 扩展抽盒机主表：增加审核状态、流水统计字段
-- 注：若 blind_box_machine 已存在，请使用 ALTER TABLE 增量更新
CREATE TABLE IF NOT EXISTS `blind_box_machine` (
    `machine_id` VARCHAR(64) NOT NULL COMMENT '抽盒机ID',
    `sale_series_id` VARCHAR(64) NOT NULL COMMENT '关联的销售系列ID',
    `shop_id` VARCHAR(64) NOT NULL COMMENT '关联的店铺ID',
    `machine_name` VARCHAR(255) NOT NULL COMMENT '抽盒机名称',
    `machine_description` TEXT COMMENT '抽盒机描述',
    `machine_cover_image` TEXT COMMENT '抽盒机封面图',
    `draw_price` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '单次抽盒价格',
    `ten_draw_price` DECIMAL(10,2) DEFAULT NULL COMMENT '十连抽价格',
    `machine_status` VARCHAR(20) NOT NULL DEFAULT 'INACTIVE' COMMENT '运行状态: ACTIVE启用/INACTIVE停用/TAKEDOWN强制下架',
    `audit_status` VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '审核状态: DRAFT草稿/PENDING待审核/APPROVED已通过/REJECTED已驳回',
    `audit_remark` VARCHAR(500) DEFAULT NULL COMMENT '审核备注（驳回原因/下架原因）',
    `audited_at` DATETIME DEFAULT NULL COMMENT '最近审核时间',
    `total_stock` INT NOT NULL DEFAULT 0 COMMENT '总库存',
    `total_draws` INT NOT NULL DEFAULT 0 COMMENT '已抽取次数',
    `total_revenue` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '累计流水',
    `guarantee_draws` INT NOT NULL DEFAULT 0 COMMENT '保底次数(0表示无保底)',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序权重',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`machine_id`),
    KEY `idx_sale_series_id` (`sale_series_id`),
    KEY `idx_shop_id` (`shop_id`),
    KEY `idx_machine_status` (`machine_status`),
    KEY `idx_audit_status` (`audit_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='抽盒机表';

-- 增量更新语句（针对已存在的表）
-- ALTER TABLE `blind_box_machine`
--   ADD COLUMN `audit_status` VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '审核状态' AFTER `machine_status`,
--   ADD COLUMN `audit_remark` VARCHAR(500) DEFAULT NULL COMMENT '审核备注' AFTER `audit_status`,
--   ADD COLUMN `audited_at` DATETIME DEFAULT NULL COMMENT '审核时间' AFTER `audit_remark`,
--   ADD COLUMN `total_revenue` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '累计流水' AFTER `total_draws`,
--   ADD INDEX `idx_audit_status` (`audit_status`);

-- 修复旧数据：将已 ACTIVE 但 audit_status 为 DRAFT 的旧抽盒机自动标记为 APPROVED
-- UPDATE `blind_box_machine` SET `audit_status` = 'APPROVED', `audited_at` = NOW() WHERE `machine_status` = 'ACTIVE' AND `audit_status` = 'DRAFT';

-- 2. 抽盒记录表（保持兼容，新增 order_no 字段以便查询展示）
CREATE TABLE IF NOT EXISTS `blind_box_draw_record` (
    `record_id` VARCHAR(64) NOT NULL COMMENT '记录ID',
    `machine_id` VARCHAR(64) NOT NULL COMMENT '抽盒机ID',
    `user_id` VARCHAR(64) NOT NULL COMMENT '用户ID',
    `sale_variant_id` VARCHAR(64) NOT NULL COMMENT '抽中的销售款式ID',
    `variant_id` VARCHAR(64) DEFAULT NULL COMMENT '原始产品ID',
    `draw_type` VARCHAR(10) NOT NULL DEFAULT 'SINGLE' COMMENT '抽盒类型: SINGLE/TEN',
    `order_id` VARCHAR(64) DEFAULT NULL COMMENT '关联的订单ID',
    `order_no` VARCHAR(64) DEFAULT NULL COMMENT '关联的订单号（冗余字段，便于展示）',
    `is_hidden` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否为隐藏款',
    `is_guaranteed` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否触发保底',
    `draw_price` DECIMAL(10,2) NOT NULL COMMENT '抽盒时价格',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '抽盒时间',
    PRIMARY KEY (`record_id`),
    KEY `idx_machine_id` (`machine_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_sale_variant_id` (`sale_variant_id`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='抽盒记录表';

-- 3. 新增：抽盒机款式覆盖配置表
-- 用于商家为单个抽盒机覆盖 sale_variant 的库存和概率（默认复用商城数据）
CREATE TABLE IF NOT EXISTS `blind_box_machine_variant` (
    `id` VARCHAR(64) NOT NULL COMMENT '主键ID',
    `machine_id` VARCHAR(64) NOT NULL COMMENT '抽盒机ID',
    `sale_variant_id` VARCHAR(64) NOT NULL COMMENT '销售款式ID',
    `override_stock` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否覆盖库存: 0否/1是',
    `stock_quantity` INT DEFAULT NULL COMMENT '覆盖的库存数量（override_stock=1时生效）',
    `override_probability` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否覆盖概率: 0否/1是',
    `draw_probability` DECIMAL(6,4) DEFAULT NULL COMMENT '覆盖的抽出概率（0-1，override_probability=1时生效）',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_machine_variant` (`machine_id`, `sale_variant_id`),
    KEY `idx_machine_id` (`machine_id`),
    KEY `idx_sale_variant_id` (`sale_variant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='抽盒机款式覆盖配置表';
