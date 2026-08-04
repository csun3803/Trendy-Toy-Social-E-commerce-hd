package com.example.trendytoysocialecommercehd.dto;

import com.example.trendytoysocialecommercehd.entity.Order;
import com.example.trendytoysocialecommercehd.entity.OrderItem;

import java.util.List;

public class OrderDetailDTO extends Order {
    private List<OrderItem> orderItems;
    private String shopId;
    private String shopName;

    public List<OrderItem> getOrderItems() {
        return orderItems;
    }

    public void setOrderItems(List<OrderItem> orderItems) {
        this.orderItems = orderItems;
    }

    public String getShopId() {
        return shopId;
    }

    public void setShopId(String shopId) {
        this.shopId = shopId;
    }

    public String getShopName() {
        return shopName;
    }

    public void setShopName(String shopName) {
        this.shopName = shopName;
    }
}