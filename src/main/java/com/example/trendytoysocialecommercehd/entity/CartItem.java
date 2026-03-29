package com.example.trendytoysocialecommercehd.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("cart_item")
public class CartItem {
    @TableId(value = "cart_item_id", type = IdType.INPUT)
    private String cartItemId;

    @TableField("user_id")
    private String userId;

    @TableField("shop_id")
    private String shopId;

    @TableField("sale_series_id")
    private String saleSeriesId;

    @TableField("sale_variant_id")
    private String saleVariantId;

    @TableField("variant_id")
    private String variantId;

    @TableField("product_snapshot")
    private String productSnapshot;

    private Integer quantity;

    @TableField("is_selected")
    private Boolean isSelected;

    @TableField("added_at")
    private LocalDateTime addedAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableField("expire_at")
    private LocalDateTime expireAt;

    @TableField("source_type")
    private String sourceType;

    @TableField("source_id")
    private String sourceId;
}