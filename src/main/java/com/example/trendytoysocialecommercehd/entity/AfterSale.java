package com.example.trendytoysocialecommercehd.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("after_sale")
public class AfterSale {
    @TableId(value = "after_sale_id", type = IdType.INPUT)
    private String afterSaleId;

    @TableField("order_id")
    private String orderId;

    @TableField("order_item_id")
    private String orderItemId;

    @TableField("user_id")
    private String userId;

    @TableField("seller_id")
    private String sellerId;

    @TableField("after_sale_type")
    private String afterSaleType;

    @TableField("after_sale_status")
    private String afterSaleStatus;

    @TableField("reason")
    private String reason;

    @TableField("description")
    private String description;

    @TableField("refund_amount")
    private BigDecimal refundAmount;

    @TableField("return_logistics_company")
    private String returnLogisticsCompany;

    @TableField("return_tracking_number")
    private String returnTrackingNumber;

    @TableField("return_address")
    private String returnAddress;

    @TableField("timeout_auto_approve_time")
    private LocalDateTime timeoutAutoApproveTime;

    @TableField("platform_intervention_reason")
    private String platformInterventionReason;

    @TableField("platform_intervention_time")
    private LocalDateTime platformInterventionTime;

    @TableField("platform_admin_id")
    private String platformAdminId;

    @TableField("platform_arbitration_result")
    private String platformArbitrationResult;

    @TableField("platform_arbitration_reason")
    private String platformArbitrationReason;

    @TableField("platform_arbitration_time")
    private LocalDateTime platformArbitrationTime;

    @TableField("reject_reason")
    private String rejectReason;

    @TableField("audit_time")
    private LocalDateTime auditTime;

    @TableField("complete_time")
    private LocalDateTime completeTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
