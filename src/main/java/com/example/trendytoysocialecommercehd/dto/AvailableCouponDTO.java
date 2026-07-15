package com.example.trendytoysocialecommercehd.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 下单页可用券对象（含实时扣减后的金额预估）
 */
@Data
public class AvailableCouponDTO {
    private String userCouponId;
    private String templateId;
    private String templateName;
    private BigDecimal discountValue;
    private BigDecimal minSpend;
    private LocalDate expiresAt;
    private String couponCode;

    /** 当前订单金额是否满足门槛 */
    private Boolean usable;
    /** 使用该券后扣减金额（不满足门槛则为 0） */
    private BigDecimal discountAmount;
    /** 使用该券后实付金额 */
    private BigDecimal payableAmount;
}
