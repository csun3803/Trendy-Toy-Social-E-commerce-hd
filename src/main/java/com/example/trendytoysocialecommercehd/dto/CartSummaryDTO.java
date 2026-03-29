package com.example.trendytoysocialecommercehd.dto;

import lombok.Data;

@Data
public class CartSummaryDTO {
    private Integer totalItems;
    private Integer selectedItems;
    private java.math.BigDecimal totalAmount;
    private java.math.BigDecimal selectedAmount;
}