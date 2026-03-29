package com.example.trendytoysocialecommercehd.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("sale_variant")
public class SaleVariant {
    @TableId(value = "sale_variant_id", type = IdType.INPUT)
    private String saleVariantId;

    private String saleSeriesId;

    private String variantId;

    private String shopId;

    private BigDecimal salePrice;

    private BigDecimal crossedPrice;

    private Integer stockQuantity;

    private Integer warningStock;

    private String skuCode;

    private String saleStatus;

    private Integer limitQuantity;

    private String customDescription;

    private String customImages;

    private Integer salesCount;

    @TableField(fill = FieldFill.INSERT)
    private Date createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updatedAt;
}
