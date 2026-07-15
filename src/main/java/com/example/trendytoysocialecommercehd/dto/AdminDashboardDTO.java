package com.example.trendytoysocialecommercehd.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class AdminDashboardDTO {
    private Integer totalUsers;
    private Integer todayNewUsers;
    private BigDecimal totalRevenue;
    private BigDecimal todayRevenue;
    private Integer totalMerchants;
    private List<SalesTrendItem> salesTrend;
    private List<OrderTrendItem> orderTrend;
    private List<MetricItem> merchantMetrics;
    private List<MetricItem> userContentMetrics;
    private List<PendingTaskItem> pendingTasks;
    private List<HealthItem> healthStatus;
    private List<HotProductItem> hotProducts;

    @Data
    public static class SalesTrendItem {
        private String date;
        private BigDecimal sales;
    }

    @Data
    public static class OrderTrendItem {
        private String date;
        private Integer orders;
    }

    @Data
    public static class MetricItem {
        private String name;
        private Object value;
        private String type;
    }

    @Data
    public static class PendingTaskItem {
        private Integer id;
        private String title;
        private Integer count;
        private String type;
    }

    @Data
    public static class HealthItem {
        private String name;
        private String value;
        private String normalRange;
        private String status;
    }

    @Data
    public static class HotProductItem {
        private Integer rank;
        private String productName;
        private String productImage;
        private Integer salesCount;
        private BigDecimal totalSales;
    }
}
