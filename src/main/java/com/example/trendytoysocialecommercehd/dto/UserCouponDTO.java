package com.example.trendytoysocialecommercehd.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户券视图对象（包含模板冗余信息，便于前端展示）
 */
@Data
public class UserCouponDTO {
    private String userCouponId;
    private String userId;
    private String templateId;
    private String couponCode;
    private String status;          // unused/used/expired/revoked
    private LocalDateTime claimedAt;
    private LocalDateTime usedAt;
    private LocalDate expiresAt;
    private String orderId;

    // 模板冗余信息
    private String templateName;
    private String type;
    private BigDecimal discountValue;
    private BigDecimal minSpend;

    // 用户冗余信息（管理端列表展示用）
    private String username;
    private String phoneNumber;
}
