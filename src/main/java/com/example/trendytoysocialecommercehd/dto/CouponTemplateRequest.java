package com.example.trendytoysocialecommercehd.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 创建/更新优惠券模板请求
 */
@Data
public class CouponTemplateRequest {
    private String templateId; // 更新时必填
    private String name;
    private String type;             // 默认 FULL_REDUCTION
    private BigDecimal discountValue;
    private BigDecimal minSpend;
    private LocalDate validFrom;
    private LocalDate validTo;
    private Integer validDays;       // 发券后有效天数
    private Integer totalQuantity;   // 0 表示不限
    private Integer userLimit;       // 每人限领
    private String status;           // active / inactive
}
