package com.example.trendytoysocialecommercehd.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AfterSaleInfoDTO {
    private String afterSaleId;
    private String orderId;
    private String orderItemId;
    private String userId;
    private String sellerId;
    private String afterSaleType; // REFUND, RETURN
    // PENDING, APPROVED, REJECTED, COMPLETED, PLATFORM_REVIEWING, PLATFORM_RESOLVED
    private String afterSaleStatus;
    private String reason;
    private String description;
    private BigDecimal refundAmount;
    private String returnLogisticsCompany;
    private String returnTrackingNumber;
    private String returnAddress;
    private LocalDateTime timeoutAutoApproveTime;
    private String platformInterventionReason;
    private LocalDateTime platformInterventionTime;
    private String platformAdminId;
    // USER (支持用户), SELLER (支持商家)
    private String platformArbitrationResult;
    private String platformArbitrationReason;
    private LocalDateTime platformArbitrationTime;
    private String rejectReason;
    private String evidenceImages;
    private LocalDateTime returnDeadline;
    private String afterSaleNo;
    private LocalDateTime auditTime;
    private LocalDateTime completeTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    // 商品信息
    private String productName;
    private String productImage;
    private String productSpec;

    // 关联的订单号/店铺名/用户名（用于管理端展示）
    private String orderNo;
    private String shopName;
    private String shopId;
    private String username;
    private String userAvatar;
}
