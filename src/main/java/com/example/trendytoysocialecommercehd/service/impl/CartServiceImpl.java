package com.example.trendytoysocialecommercehd.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.trendytoysocialecommercehd.dto.AddToCartRequest;
import com.example.trendytoysocialecommercehd.dto.CartSummaryDTO;
import com.example.trendytoysocialecommercehd.dto.UpdateCartRequest;
import com.example.trendytoysocialecommercehd.entity.CartItem;
import com.example.trendytoysocialecommercehd.mapper.CartMapper;
import com.example.trendytoysocialecommercehd.service.CartService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private CartMapper cartMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public List<CartItem> getCartByUserId(String userId) {
        LambdaQueryWrapper<CartItem> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CartItem::getUserId, userId)
                .orderByDesc(CartItem::getAddedAt);
        return cartMapper.selectList(queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CartItem addToCart(AddToCartRequest request) {
        LambdaQueryWrapper<CartItem> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CartItem::getUserId, request.getUserId())
                .eq(CartItem::getSaleVariantId, request.getSaleVariantId());

        CartItem existingItem = cartMapper.selectOne(queryWrapper);

        if (existingItem != null) {
            existingItem.setQuantity(existingItem.getQuantity() + (request.getQuantity() != null ? request.getQuantity() : 1));
            existingItem.setUpdatedAt(LocalDateTime.now());
            cartMapper.updateById(existingItem);
            return existingItem;
        }

        CartItem cartItem = new CartItem();
        cartItem.setCartItemId("cart_" + UUID.randomUUID().toString().substring(0, 8));
        cartItem.setUserId(request.getUserId());
        cartItem.setShopId(request.getShopId());
        cartItem.setSaleSeriesId(request.getSaleSeriesId());
        cartItem.setSaleVariantId(request.getSaleVariantId());
        cartItem.setVariantId(request.getVariantId());

        try {
            cartItem.setProductSnapshot(request.getProductSnapshot());
        } catch (Exception e) {
            cartItem.setProductSnapshot("{}");
        }

        cartItem.setQuantity(request.getQuantity() != null ? request.getQuantity() : 1);
        cartItem.setIsSelected(true);
        cartItem.setAddedAt(LocalDateTime.now());
        cartItem.setUpdatedAt(LocalDateTime.now());
        cartItem.setExpireAt(LocalDateTime.now().plusDays(30));
        cartItem.setSourceType(request.getSourceType() != null ? request.getSourceType() : "manual");
        cartItem.setSourceId(request.getSourceId() != null ? request.getSourceId() : "");

        cartMapper.insert(cartItem);
        return cartItem;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CartItem updateCartItem(UpdateCartRequest request) {
        CartItem cartItem = cartMapper.selectById(request.getCartItemId());
        if (cartItem == null) {
            throw new RuntimeException("购物车商品不存在");
        }

        if (request.getQuantity() != null) {
            cartItem.setQuantity(request.getQuantity());
        }
        if (request.getIsSelected() != null) {
            cartItem.setIsSelected(request.getIsSelected());
        }
        cartItem.setUpdatedAt(LocalDateTime.now());

        cartMapper.updateById(cartItem);
        return cartItem;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeCartItem(String cartItemId) {
        cartMapper.deleteById(cartItemId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeSelectedItems(String userId) {
        cartMapper.deleteSelectedByUserId(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void selectAllItems(String userId, Boolean isSelected) {
        cartMapper.updateAllSelected(userId, isSelected);
    }

    @Override
    public CartSummaryDTO getCartSummary(String userId) {
        LambdaQueryWrapper<CartItem> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CartItem::getUserId, userId).orderByDesc(CartItem::getAddedAt);
        List<CartItem> items = cartMapper.selectList(queryWrapper);

        CartSummaryDTO summary = new CartSummaryDTO();
        summary.setTotalItems(items.size());

        int selectedCount = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal selectedAmount = BigDecimal.ZERO;

        for (CartItem item : items) {
            try {
                com.fasterxml.jackson.databind.JsonNode snapshot = objectMapper.readTree(item.getProductSnapshot());
                BigDecimal price = new BigDecimal(snapshot.get("price").asText());
                BigDecimal itemTotal = price.multiply(new BigDecimal(item.getQuantity()));
                totalAmount = totalAmount.add(itemTotal);

                if (Boolean.TRUE.equals(item.getIsSelected())) {
                    selectedCount++;
                    selectedAmount = selectedAmount.add(itemTotal);
                }
            } catch (Exception e) {
                // 忽略解析错误
            }
        }

        summary.setSelectedItems(selectedCount);
        summary.setTotalAmount(totalAmount);
        summary.setSelectedAmount(selectedAmount);

        return summary;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearCart(String userId) {
        cartMapper.deleteByUserId(userId);
    }
}