package com.example.trendytoysocialecommercehd.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("COUPON_TEMPLATE")
public class CouponTemplate {
    @TableId(value = "template_id", type = IdType.INPUT)
    private String templateId;

    private String name;

    private String type;

    @TableField("discount_value")
    private BigDecimal discountValue;

    @TableField("min_spend")
    private BigDecimal minSpend;

    @TableField("valid_from")
    private LocalDate validFrom;

    @TableField("valid_to")
    private LocalDate validTo;

    @TableField("valid_days")
    private Integer validDays;

    @TableField("total_quantity")
    private Integer totalQuantity;

    @TableField("user_limit")
    private Integer userLimit;

    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
