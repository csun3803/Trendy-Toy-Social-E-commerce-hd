-- 创建售后表
CREATE TABLE IF NOT EXISTS after_sale (
    after_sale_id VARCHAR(255) PRIMARY KEY COMMENT '售后ID',
    order_id VARCHAR(255) NOT NULL COMMENT '订单ID',
    order_item_id VARCHAR(255) NOT NULL COMMENT '订单项ID',
    user_id VARCHAR(255) NOT NULL COMMENT '用户ID',
    seller_id VARCHAR(255) NOT NULL COMMENT '卖家ID',
    after_sale_type VARCHAR(50) NOT NULL COMMENT '售后类型: REFUND, RETURN',
    after_sale_status VARCHAR(50) NOT NULL COMMENT '售后状态: PENDING, APPROVED, REJECTED, COMPLETED',
    reason VARCHAR(255) NOT NULL COMMENT '售后原因',
    description TEXT COMMENT '详细描述',
    refund_amount DECIMAL(10,2) NOT NULL COMMENT '退款金额',
    return_logistics_company VARCHAR(100) COMMENT '退货物流公司',
    return_tracking_number VARCHAR(100) COMMENT '退货运单号',
    reject_reason TEXT COMMENT '拒绝原因',
    audit_time DATETIME COMMENT '审核时间',
    complete_time DATETIME COMMENT '完成时间',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    INDEX idx_order_id (order_id),
    INDEX idx_user_id (user_id),
    INDEX idx_seller_id (seller_id),
    INDEX idx_status (after_sale_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='售后表';
