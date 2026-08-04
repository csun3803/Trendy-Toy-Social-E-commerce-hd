package com.example.trendytoysocialecommercehd.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("shipping_template")
public class ShippingTemplate {
    @TableId(value = "template_id", type = IdType.INPUT)
    private String templateId;

    @TableField("shop_id")
    private String shopId;

    @TableField("template_name")
    private String templateName;

    @TableField("free_shipping_threshold")
    private BigDecimal freeShippingThreshold;

    @TableField("default_fee")
    private BigDecimal defaultFee;

    @TableField("regional_rules")
    private String regionalRules;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
