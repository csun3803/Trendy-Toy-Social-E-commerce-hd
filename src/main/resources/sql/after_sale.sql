-- 创建售后表
CREATE TABLE IF NOT EXISTS after_sale (
    after_sale_id VARCHAR(255) PRIMARY KEY COMMENT '售后ID',
    order_id VARCHAR(255) NOT NULL COMMENT '订单ID',
    order_item_id VARCHAR(255) NOT NULL COMMENT '订单项ID',
    user_id VARCHAR(255) NOT NULL COMMENT '用户ID',
    seller_id VARCHAR(255) NOT NULL COMMENT '卖家ID',
    after_sale_type VARCHAR(50) NOT NULL COMMENT '售后类型: REFUND, RETURN',
    after_sale_status VARCHAR(50) NOT NULL COMMENT '售后状态: PENDING, APPROVED, REJECTED, COMPLETED, CLOSED',
    reason VARCHAR(255) NOT NULL COMMENT '售后原因',
    description TEXT COMMENT '详细描述',
    evidence_images TEXT COMMENT '凭证图片JSON数组',
    refund_amount DECIMAL(10,2) NOT NULL COMMENT '退款金额',
    return_logistics_company VARCHAR(100) COMMENT '退货物流公司',
    return_tracking_number VARCHAR(100) COMMENT '退货运单号',
    return_address TEXT COMMENT '退货地址',
    return_deadline DATETIME COMMENT '退货截止时间(7天)',
    after_sale_no VARCHAR(50) COMMENT '售后单号',
    timeout_auto_approve_time DATETIME COMMENT '商家超时自动同意截止时间',
    platform_intervention_reason TEXT COMMENT '平台介入原因',
    platform_intervention_time DATETIME COMMENT '平台介入时间',
    platform_admin_id VARCHAR(255) COMMENT '平台管理员ID',
    platform_arbitration_result VARCHAR(20) COMMENT '平台裁决结果: USER/SELLER',
    platform_arbitration_reason TEXT COMMENT '平台裁决原因',
    platform_arbitration_time DATETIME COMMENT '平台裁决时间',
    reject_reason TEXT COMMENT '拒绝原因',
    audit_time DATETIME COMMENT '审核时间',
    complete_time DATETIME COMMENT '完成时间',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    INDEX idx_order_id (order_id),
    INDEX idx_user_id (user_id),
    INDEX idx_seller_id (seller_id),
    INDEX idx_status (after_sale_status),
    INDEX idx_after_sale_no (after_sale_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='售后表';

-- 增量更新：为已有表增加新字段
-- ALTER TABLE after_sale ADD COLUMN evidence_images TEXT COMMENT '凭证图片JSON数组';
-- ALTER TABLE after_sale ADD COLUMN return_deadline DATETIME COMMENT '退货截止时间(7天)';
-- ALTER TABLE after_sale ADD COLUMN after_sale_no VARCHAR(50) COMMENT '售后单号';
-- ALTER TABLE after_sale ADD INDEX idx_after_sale_no (after_sale_no);
