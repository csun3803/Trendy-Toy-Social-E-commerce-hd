package com.example.trendytoysocialecommercehd.dto;

import lombok.Data;
import java.util.List;

@Data
public class BatchCreateOrderRequest {
    private String userId;
    private String addressId;
    private String userRemark;
    private String paymentMethod;
    private List<ShopOrderRequest> shopOrders;

    /** 用户优惠券ID（可选，下单时使用，对整批订单生效） */
    private String userCouponId;

    @Data
    public static class ShopOrderRequest {
        private String shopId;
        private List<OrderItemRequest> items;
        private java.math.BigDecimal amount;
        private java.math.BigDecimal shippingFee;
        private java.math.BigDecimal totalDiscount;
        private java.math.BigDecimal actualAmount;
    }
}
