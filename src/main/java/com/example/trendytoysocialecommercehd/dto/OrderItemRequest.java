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
}