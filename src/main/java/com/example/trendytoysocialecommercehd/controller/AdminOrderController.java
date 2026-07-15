package com.example.trendytoysocialecommercehd.controller;

import com.example.trendytoysocialecommercehd.annotation.AuditLog;
import com.example.trendytoysocialecommercehd.common.Result;
import com.example.trendytoysocialecommercehd.dto.AfterSaleInfoDTO;
import com.example.trendytoysocialecommercehd.entity.AfterSale;
import com.example.trendytoysocialecommercehd.entity.Order;
import com.example.trendytoysocialecommercehd.entity.OrderItem;
import com.example.trendytoysocialecommercehd.entity.Shop;
import com.example.trendytoysocialecommercehd.entity.User;
import com.example.trendytoysocialecommercehd.mapper.OrderItemMapper;
import com.example.trendytoysocialecommercehd.mapper.ShopMapper;
import com.example.trendytoysocialecommercehd.mapper.UserMapper;
import com.example.trendytoysocialecommercehd.service.AfterSaleService;
import com.example.trendytoysocialecommercehd.util.JwtUtil;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 平台管理端：订单管理 & 售后管理（监管视角）
 * 订单管理：仅查看和导出，不支持发货等操作
 * 售后管理：日常仅查看；当用户申请平台介入时，可进行仲裁
 */
@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

    @Autowired
    private AfterSaleService afterSaleService;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private ShopMapper shopMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 平台订单列表（监管视角：仅查看，不支持发货等操作）
     */
    @GetMapping("/list")
    public Result<?> listOrders(
            @RequestParam(required = false) String sellerId,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        try {
            List<Order> orders = afterSaleService.getAllOrders(sellerId, userId, status, startTime, endTime);
            // 关键词搜索订单号
            if (orderNo != null && !orderNo.isEmpty()) {
                List<Order> filtered = new ArrayList<>();
                for (Order o : orders) {
                    if (o.getOrderNo() != null && o.getOrderNo().toLowerCase().contains(orderNo.toLowerCase())) {
                        filtered.add(o);
                    }
                }
                orders = filtered;
            }
            // 为每个订单填充商品项、店铺名、用户名
            List<Map<String, Object>> result = new ArrayList<>();
            for (Order order : orders) {
                Map<String, Object> item = new HashMap<>();
                item.put("order", order);
                // 订单项
                Map<String, Object> params = new HashMap<>();
                params.put("order_id", order.getOrderId());
                List<OrderItem> items = orderItemMapper.selectByMap(params);
                item.put("orderItems", items);
                // 店铺名（取第一个订单项的卖家）
                if (items != null && !items.isEmpty()) {
                    String sid = items.get(0).getItemSellerId();
                    if (sid != null) {
                        Shop shop = shopMapper.selectById(sid);
                        if (shop != null) {
                            item.put("shopName", shop.getShopName());
                            item.put("shopId", shop.getShopId());
                        }
                    }
                }
                // 用户名
                if (order.getUserId() != null) {
                    User user = userMapper.selectById(order.getUserId());
                    if (user != null) {
                        Map<String, Object> userInfo = new HashMap<>();
                        userInfo.put("username", user.getUsername());
                        userInfo.put("avatarUrl", user.getAvatarUrl());
                        item.put("userInfo", userInfo);
                    }
                }
                result.add(item);
            }
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("获取订单列表失败: " + e.getMessage());
        }
    }

    /**
     * 平台售后列表（支持过滤）
     */
    @GetMapping("/after-sales/list")
    public Result<?> listAfterSales(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String sellerId,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        try {
            List<AfterSaleInfoDTO> list = afterSaleService.getAllAfterSales(status, sellerId, userId, startTime, endTime);
            return Result.success(list);
        } catch (Exception e) {
            return Result.error("获取售后列表失败: " + e.getMessage());
        }
    }

    /**
     * 平台介入申请列表：所有状态为 PLATFORM_REVIEWING 的售后单
     */
    @GetMapping("/after-sales/intervention")
    public Result<?> platformInterventionList() {
        try {
            List<AfterSaleInfoDTO> list = afterSaleService.getPlatformInterventionList();
            return Result.success(list);
        } catch (Exception e) {
            return Result.error("获取平台介入列表失败: " + e.getMessage());
        }
    }

    /**
     * 平台仲裁：管理员在售后单详情页做出最终裁决
     * 裁决后商家和用户都无法再操作
     */
    @AuditLog(module = "ADMIN", action = "ARBITRATE", description = "平台仲裁售后申请")
    @PostMapping("/after-sales/{afterSaleId}/arbitrate")
    public Result<?> arbitrate(
            @PathVariable String afterSaleId,
            @RequestBody ArbitrateRequest request,
            @RequestHeader("Authorization") String token) {
        try {
            String cleanToken = token.replace("Bearer ", "");
            String adminId;
            try {
                adminId = jwtUtil.getUserIdFromToken(cleanToken);
            } catch (Exception e) {
                adminId = "system";
            }
            AfterSale afterSale = afterSaleService.arbitrateAfterSale(afterSaleId, request.getResult(), request.getReason(), adminId);
            return Result.success(afterSale);
        } catch (Exception e) {
            return Result.error("平台仲裁失败: " + e.getMessage());
        }
    }

    @Data
    public static class ArbitrateRequest {
        // "USER" 支持用户 / "SELLER" 支持商家
        private String result;
        private String reason;
    }
}
