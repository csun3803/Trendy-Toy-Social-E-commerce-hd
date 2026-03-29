package com.example.trendytoysocialecommercehd.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("orders")
public class Order {
    @TableId(value = "order_id", type = IdType.INPUT)
    private String orderId;

    @TableField("order_no")
    private String orderNo;

    @TableField("user_id")
    private String userId;

    private BigDecimal amount;

    @TableField("shipping_fee")
    private BigDecimal shippingFee;

    @TableField("total_discount")
    private BigDecimal totalDiscount;

    @TableField("actual_amount")
    private BigDecimal actualAmount;

    @TableField("settlement_amount")
    private BigDecimal settlementAmount;

    @TableField("platform_commission")
    private BigDecimal platformCommission;

    @TableField("refund_amount")
    private BigDecimal refundAmount;

    @TableField("payment_method")
    private String paymentMethod;

    @TableField("payment_status")
    private String paymentStatus;

    @TableField("payment_time")
    private LocalDateTime paymentTime;

    @TableField("address_id")
    private String addressId;

    @TableField("logistics_company")
    private String logisticsCompany;

    @TableField("tracking_number")
    private String trackingNumber;

    @TableField("shipping_status")
    private String shippingStatus;

    @TableField("shipped_time")
    private LocalDateTime shippedTime;

    @TableField("received_time")
    private LocalDateTime receivedTime;

    @TableField("logistics_tracking")
    private String logisticsTracking;

    @TableField("estimated_delivery")
    private LocalDateTime estimatedDelivery;

    @TableField("order_status")
    private String orderStatus;

    @TableField("cancel_reason")
    private String cancelReason;

    @TableField("cancel_time")
    private LocalDateTime cancelTime;

    @TableField("complete_time")
    private LocalDateTime completeTime;

    @TableField("total_quantity")
    private Integer totalQuantity;

    @TableField("product_variety_count")
    private Integer productVarietyCount;

    @TableField("after_sales_status")
    private String afterSalesStatus;

    @TableField("after_sales_time")
    private LocalDateTime afterSalesTime;

    @TableField("last_after_sales_time")
    private LocalDateTime lastAfterSalesTime;

    @TableField("user_remark")
    private String userRemark;

    @TableField("payment_deadline")
    private LocalDateTime paymentDeadline;

    @TableField("auto_cancel_time")
    private LocalDateTime autoCancelTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

}