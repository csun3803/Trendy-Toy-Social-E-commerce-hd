package com.example.trendytoysocialecommercehd.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("order_items")
public class OrderItem {
    @TableId(value = "order_item_id", type = IdType.INPUT)
    private String orderItemId;

    @TableField("order_id")
    private String orderId;

    @TableField("product_id")
    private String productId;

    @TableField("original_price")
    private BigDecimal originalPrice;

    @TableField("unit_price")
    private BigDecimal unitPrice;

    private Integer quantity;

    @TableField("subtotal_amount")
    private BigDecimal subtotalAmount;

    @TableField("allocated_discount")
    private BigDecimal allocatedDiscount;

    @TableField("actual_subtotal")
    private BigDecimal actualSubtotal;

    @TableField("item_after_sales_status")
    private String itemAfterSalesStatus;

    @TableField("item_refund_amount")
    private BigDecimal itemRefundAmount;

    @TableField("refund_quantity")
    private Integer refundQuantity;

    @TableField("item_seller_id")
    private String itemSellerId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}