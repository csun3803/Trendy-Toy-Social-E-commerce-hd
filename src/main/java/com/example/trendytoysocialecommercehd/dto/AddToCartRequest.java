package com.example.trendytoysocialecommercehd.dto;

import lombok.Data;

@Data
public class AddToCartRequest {
    private String userId;
    private String shopId;
    private String saleSeriesId;
    private String saleVariantId;
    private String variantId;
    private String productSnapshot;
    private Integer quantity;
    private String sourceType;
    private String sourceId;
}