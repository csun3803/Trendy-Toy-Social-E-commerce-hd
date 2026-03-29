package com.example.trendytoysocialecommercehd.service;

import com.example.trendytoysocialecommercehd.dto.AddToCartRequest;
import com.example.trendytoysocialecommercehd.dto.CartSummaryDTO;
import com.example.trendytoysocialecommercehd.dto.UpdateCartRequest;
import com.example.trendytoysocialecommercehd.entity.CartItem;

import java.util.List;

public interface CartService {
    List<CartItem> getCartByUserId(String userId);
    CartItem addToCart(AddToCartRequest request);
    CartItem updateCartItem(UpdateCartRequest request);
    void removeCartItem(String cartItemId);
    void removeSelectedItems(String userId);
    void selectAllItems(String userId, Boolean isSelected);
    CartSummaryDTO getCartSummary(String userId);
    void clearCart(String userId);
}