package com.example.trendytoysocialecommercehd.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class DashboardDataDTO {
    private BigDecimal todaySales;
    private Integer todayOrders;
    private Integer pendingShipment;
    private Integer afterSales;
    private Integer productCount;
    private List<SalesTrendItem> salesTrend;
    private List<HotProductItem> hotProducts;
    private List<TaskItem> tasks;

    @Data
    public static class SalesTrendItem {
        private String date;
        private BigDecimal sales;
        private Integer orders;
    }

    @Data
    public static class HotProductItem {
        private Integer rank;
        private String name;
        private Integer sales;
        private BigDecimal amount;
        private String image;
    }

    @Data
    public static class TaskItem {
        private Integer id;
        private String title;
        private Integer count;
        private String type;
    }
}