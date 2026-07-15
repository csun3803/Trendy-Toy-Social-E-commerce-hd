package com.example.trendytoysocialecommercehd.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class OrderItemRequest {
    private String productId;
    private BigDecimal originalPrice;
    private BigDecimal unitPrice;
    private Integer quantity;
    private BigDecimal subtotalAmount;
    private BigDecimal allocatedDiscount;
    private BigDecimal actualSubtotal;
    private String itemSellerId;
    // 新增字段 - 商品快照信息
    private String productName;
    private String productImage;
    private String productSpec;
}