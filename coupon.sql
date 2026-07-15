-- 20. 优惠券模板表
-- 说明：在原始 schema 基础上新增 valid_days（有效天数）字段，
-- 用于发券时计算每个用户券实例的 expires_at = claimed_at + valid_days
CREATE TABLE IF NOT EXISTS COUPON_TEMPLATE (
    template_id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '模板名称',
    type VARCHAR(20) NOT NULL DEFAULT 'FULL_REDUCTION' COMMENT '券类型: FULL_REDUCTION(满减)',
    discount_value DECIMAL(10,2) NOT NULL COMMENT '减扣金额',
    min_spend DECIMAL(10,2) DEFAULT 0 COMMENT '满减门槛',
    valid_from DATE NOT NULL COMMENT '模板生效日期',
    valid_to DATE NOT NULL COMMENT '模板失效日期',
    valid_days INT NOT NULL DEFAULT 30 COMMENT '发券后有效天数（用于计算每个实例的过期时间）',
    total_quantity INT NOT NULL DEFAULT 0 COMMENT '发放总量(0=不限)',
    user_limit INT DEFAULT 1 COMMENT '每人限领数量',
    status VARCHAR(20) DEFAULT 'active' COMMENT 'active/inactive',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_type (type),
    INDEX idx_status (status),
    INDEX idx_valid_date (valid_from, valid_to)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券模板表';

-- 21. 用户优惠券表
CREATE TABLE IF NOT EXISTS USER_COUPON (
    user_coupon_id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64),
    template_id VARCHAR(64),
    coupon_code VARCHAR(50) UNIQUE,
    status VARCHAR(20) DEFAULT 'unused' COMMENT 'unused/used/expired/revoked',
    claimed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '领取(发放)时间',
    used_at TIMESTAMP NULL COMMENT '使用时间',
    expires_at DATE NOT NULL COMMENT '过期日期(由模板valid_days计算)',
    order_id VARCHAR(64) COMMENT '使用该券的订单ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_template_id (template_id),
    INDEX idx_status (status),
    INDEX idx_coupon_code (coupon_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户优惠券表';
