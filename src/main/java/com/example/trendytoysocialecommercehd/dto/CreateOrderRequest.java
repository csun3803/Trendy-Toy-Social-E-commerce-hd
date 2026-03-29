package com.example.trendytoysocialecommercehd.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CreateOrderRequest {
    private String userId;
    private BigDecimal amount;
    private BigDecimal shippingFee;
    private BigDecimal totalDiscount;
    private BigDecimal actualAmount;
    private String addressId;
    private String userRemark;
    private List<OrderItemRequest> items;
}