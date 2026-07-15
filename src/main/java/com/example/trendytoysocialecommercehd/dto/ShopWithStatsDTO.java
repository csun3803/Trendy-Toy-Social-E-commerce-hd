package com.example.trendytoysocialecommercehd.dto;

import com.example.trendytoysocialecommercehd.entity.Shop;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ShopWithStatsDTO {
    private Shop shop;
    private Integer monthlySales;
    private Integer totalSales;
    private BigDecimal totalSalesAmount;

    public ShopWithStatsDTO(Shop shop, Integer monthlySales, Integer totalSales, BigDecimal totalSalesAmount) {
        this.shop = shop;
        this.monthlySales = monthlySales;
        this.totalSales = totalSales;
        this.totalSalesAmount = totalSalesAmount;
    }
}