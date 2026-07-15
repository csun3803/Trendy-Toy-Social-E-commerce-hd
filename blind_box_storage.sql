-- ============================================================
-- 暂存柜：用户抽中的盲盒暂存，可选择发货生成订单
-- ============================================================
CREATE TABLE IF NOT EXISTS `blind_box_storage` (
    `storage_id` VARCHAR(64) NOT NULL COMMENT '暂存柜记录ID',
    `user_id` VARCHAR(64) NOT NULL COMMENT '用户ID',
    `machine_id` VARCHAR(64) NOT NULL COMMENT '抽盒机ID',
    `machine_name` VARCHAR(255) DEFAULT NULL COMMENT '抽盒机名称（冗余）',
    `set_id` VARCHAR(64) DEFAULT NULL COMMENT '套盒ID',
    `slot_no` INT DEFAULT NULL COMMENT '格位号',
    `sale_variant_id` VARCHAR(64) NOT NULL COMMENT '销售款式ID',
    `variant_id` VARCHAR(64) DEFAULT NULL COMMENT '原始产品ID',
    `variant_name` VARCHAR(255) DEFAULT NULL COMMENT '款式名称（冗余）',
    `variant_image` VARCHAR(500) DEFAULT NULL COMMENT '款式图片（冗余）',
    `is_hidden` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否隐藏款',
    `draw_price` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '抽取价格',
    `pay_order_id` VARCHAR(64) DEFAULT NULL COMMENT '支付订单ID（抽盒时生成的已支付订单）',
    `status` VARCHAR(20) NOT NULL DEFAULT 'STORED' COMMENT '状态: STORED暂存中/SHIPPED已发货',
    `ship_order_id` VARCHAR(64) DEFAULT NULL COMMENT '发货订单ID（发货后生成）',
    `stored_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '存入时间',
    `shipped_at` DATETIME DEFAULT NULL COMMENT '发货时间',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`storage_id`),
    INDEX `idx_user_status` (`user_id`, `status`),
    INDEX `idx_machine` (`machine_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='盲盒暂存柜';
