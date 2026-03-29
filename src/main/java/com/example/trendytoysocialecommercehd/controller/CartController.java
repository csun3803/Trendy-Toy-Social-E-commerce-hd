package com.example.trendytoysocialecommercehd.controller;

import com.example.trendytoysocialecommercehd.common.Result;
import com.example.trendytoysocialecommercehd.dto.AddToCartRequest;
import com.example.trendytoysocialecommercehd.dto.CartSummaryDTO;
import com.example.trendytoysocialecommercehd.dto.UpdateCartRequest;
import com.example.trendytoysocialecommercehd.entity.CartItem;
import com.example.trendytoysocialecommercehd.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    @GetMapping("/user/{userId}")
    public Result<List<CartItem>> getCartByUserId(@PathVariable String userId) {
        try {
            List<CartItem> cartItems = cartService.getCartByUserId(userId);
            return Result.success(cartItems);
        } catch (Exception e) {
            return Result.error("获取购物车失败: " + e.getMessage());
        }
    }

    @PostMapping
    public Result<CartItem> addToCart(@RequestBody AddToCartRequest request) {
        try {
            CartItem cartItem = cartService.addToCart(request);
            return Result.success("添加成功", cartItem);
        } catch (Exception e) {
            return Result.error("添加到购物车失败: " + e.getMessage());
        }
    }

    @PutMapping("/{cartItemId}")
    public Result<CartItem> updateCartItem(
            @PathVariable String cartItemId,
            @RequestBody UpdateCartRequest request) {
        try {
            request.setCartItemId(cartItemId);
            CartItem cartItem = cartService.updateCartItem(request);
            return Result.success("更新成功", cartItem);
        } catch (Exception e) {
            return Result.error("更新购物车失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{cartItemId}")
    public Result<Void> removeCartItem(@PathVariable String cartItemId) {
        try {
            cartService.removeCartItem(cartItemId);
            return Result.success("删除成功", null);
        } catch (Exception e) {
            return Result.error("删除购物车商品失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/selected/{userId}")
    public Result<Void> removeSelectedItems(@PathVariable String userId) {
        try {
            cartService.removeSelectedItems(userId);
            return Result.success("删除选中商品成功", null);
        } catch (Exception e) {
            return Result.error("删除选中商品失败: " + e.getMessage());
        }
    }

    @PutMapping("/select-all/{userId}")
    public Result<Void> selectAllItems(
            @PathVariable String userId,
            @RequestBody Map<String, Boolean> body) {
        try {
            Boolean isSelected = body.get("isSelected");
            cartService.selectAllItems(userId, isSelected);
            return Result.success("操作成功", null);
        } catch (Exception e) {
            return Result.error("全选操作失败: " + e.getMessage());
        }
    }

    @GetMapping("/summary/{userId}")
    public Result<CartSummaryDTO> getCartSummary(@PathVariable String userId) {
        try {
            CartSummaryDTO summary = cartService.getCartSummary(userId);
            return Result.success(summary);
        } catch (Exception e) {
            return Result.error("获取购物车统计失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/clear/{userId}")
    public Result<Void> clearCart(@PathVariable String userId) {
        try {
            cartService.clearCart(userId);
            return Result.success("清空购物车成功", null);
        } catch (Exception e) {
            return Result.error("清空购物车失败: " + e.getMessage());
        }
    }
}