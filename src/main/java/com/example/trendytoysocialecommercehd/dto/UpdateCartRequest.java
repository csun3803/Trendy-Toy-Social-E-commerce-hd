package com.example.trendytoysocialecommercehd.dto;

import lombok.Data;

@Data
public class UpdateCartRequest {
    private String cartItemId;
    private Integer quantity;
    private Boolean isSelected;
}