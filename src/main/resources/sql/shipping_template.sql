CREATE TABLE IF NOT EXISTS shipping_template (
    template_id VARCHAR(64) PRIMARY KEY COMMENT '模板ID',
    shop_id VARCHAR(64) NOT NULL COMMENT '所属店铺ID',
    template_name VARCHAR(100) NOT NULL COMMENT '模板名称',
    free_shipping_threshold DECIMAL(10,2) DEFAULT NULL COMMENT '包邮门槛金额，NULL表示不设门槛',
    default_fee DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '默认运费',
    regional_rules JSON DEFAULT NULL COMMENT '区域运费规则JSON',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_shop_id (shop_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运费模板表';
