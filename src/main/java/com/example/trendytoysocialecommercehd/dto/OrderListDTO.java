package com.example.trendytoysocialecommercehd.dto;

import com.example.trendytoysocialecommercehd.entity.Order;
import com.example.trendytoysocialecommercehd.entity.OrderItem;

import java.util.List;

public class OrderListDTO extends Order {
    private List<OrderItem> items;
    private String shopId;
    private String shopName;
    private String buyerNickname;

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
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

    public String getBuyerNickname() {
        return buyerNickname;
    }

    public void setBuyerNickname(String buyerNickname) {
        this.buyerNickname = buyerNickname;
    }
}