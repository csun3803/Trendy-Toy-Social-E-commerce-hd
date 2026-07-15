package com.example.trendytoysocialecommercehd.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateAfterSaleRequest {
    private String orderId;
    private String orderItemId;
    private String afterSaleType; // REFUND, RETURN
    private String reason;
    private String description;
    private BigDecimal refundAmount;
}
