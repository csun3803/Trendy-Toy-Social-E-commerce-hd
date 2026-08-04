package com.example.trendytoysocialecommercehd.controller;

import com.example.trendytoysocialecommercehd.common.Result;
import com.example.trendytoysocialecommercehd.dto.CreateOrderRequest;
import com.example.trendytoysocialecommercehd.dto.BatchCreateOrderRequest;
import com.example.trendytoysocialecommercehd.dto.OrderDetailDTO;
import com.example.trendytoysocialecommercehd.dto.OrderListDTO;
import com.example.trendytoysocialecommercehd.entity.Order;
import com.example.trendytoysocialecommercehd.service.OrderService;
import com.example.trendytoysocialecommercehd.util.JwtUtil;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping
    public Result<Order> createOrder(@RequestBody CreateOrderRequest request) {
        try {
            Order order = orderService.createOrder(request);
            return Result.success(order);
        } catch (Exception e) {
            return Result.error("创建订单失败: " + e.getMessage());
        }
    }

    @PostMapping("/batch")
    public Result<List<Order>> batchCreateOrders(@RequestBody BatchCreateOrderRequest request) {
        try {
            List<Order> orders = orderService.batchCreateOrders(request);
            return Result.success(orders);
        } catch (Exception e) {
            return Result.error("批量创建订单失败: " + e.getMessage());
        }
    }

    @GetMapping("/{orderId}")
    public Result<OrderDetailDTO> getOrderDetail(@PathVariable String orderId) {
        try {
            OrderDetailDTO orderDetail = orderService.getOrderDetail(orderId);
            if (orderDetail == null) {
                return Result.error("订单不存在");
            }
            return Result.success(orderDetail);
        } catch (Exception e) {
            return Result.error("获取订单详情失败: " + e.getMessage());
        }
    }

    @GetMapping("/seller/{sellerId}")
    public Result<List<Order>> getOrdersBySeller(@PathVariable String sellerId) {
        try {
            List<Order> orders = orderService.getOrdersBySellerId(sellerId);
            return Result.success(orders);
        } catch (Exception e) {
            return Result.error("获取商家订单失败: " + e.getMessage());
        }
    }

    @PostMapping("/{orderId}/pay")
    public Result<Order> payOrder(
            @PathVariable String orderId,
            @RequestBody PayRequest payRequest) {
        try {
            Order order = orderService.payOrder(orderId, payRequest.getPaymentMethod());
            return Result.success(order);
        } catch (Exception e) {
            return Result.error("支付失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{orderId}")
    public Result<Void> deleteOrder(@PathVariable String orderId) {
        try {
            boolean deleted = orderService.deleteOrder(orderId);
            if (deleted) {
                return Result.success();
            } else {
                return Result.error("订单不存在或已删除");
            }
        } catch (Exception e) {
            return Result.error("删除订单失败: " + e.getMessage());
        }
    }

    @GetMapping("/user/{userId}")
    public Result<List<OrderListDTO>> getUserOrders(
            @PathVariable String userId,
            @RequestParam(required = false) String status) {
        try {
            List<OrderListDTO> orders = orderService.getUserOrdersWithItems(userId, status);
            return Result.success(orders);
        } catch (Exception e) {
            return Result.error("获取订单列表失败: " + e.getMessage());
        }
    }

    @PostMapping("/{orderId}/cancel")
    public Result<Order> cancelOrder(@PathVariable String orderId) {
        try {
            Order order = orderService.cancelOrder(orderId);
            return Result.success(order);
        } catch (Exception e) {
            return Result.error("取消订单失败: " + e.getMessage());
        }
    }

    @PostMapping("/{orderId}/ship")
    public Result<Order> shipOrder(
            @PathVariable String orderId,
            @RequestBody ShipRequest shipRequest) {
        try {
            Order order = orderService.shipOrder(orderId, shipRequest.getLogisticsCompany(), shipRequest.getTrackingNumber());
            return Result.success(order);
        } catch (Exception e) {
            return Result.error("发货失败: " + e.getMessage());
        }
    }

    @GetMapping("/{orderId}/logistics")
    public Result<OrderService.LogisticsInfo> getLogistics(@PathVariable String orderId) {
        try {
            OrderService.LogisticsInfo logistics = orderService.getLogisticsInfo(orderId);
            return Result.success(logistics);
        } catch (Exception e) {
            return Result.error("获取物流信息失败: " + e.getMessage());
        }
    }

    @PostMapping("/{orderId}/logistics-status")
    public Result<Order> updateLogisticsStatus(
            @PathVariable String orderId,
            @RequestBody LogisticsStatusRequest request) {
        try {
            Order order = orderService.updateLogisticsStatus(orderId, request.getStatus());
            return Result.success(order);
        } catch (Exception e) {
            return Result.error("更新物流状态失败: " + e.getMessage());
        }
    }

    @PostMapping("/{orderId}/complete")
    public Result<Order> completeOrder(@PathVariable String orderId) {
        try {
            Order order = orderService.completeOrder(orderId);
            return Result.success(order);
        } catch (Exception e) {
            return Result.error("完成订单失败: " + e.getMessage());
        }
    }

    @GetMapping("/seller/{sellerId}/with-items")
    public Result<List<OrderListDTO>> getSellerOrdersWithItems(
            @PathVariable String sellerId,
            @RequestParam(required = false) String status) {
        try {
            List<OrderListDTO> orders = orderService.getSellerOrdersWithItems(sellerId, status);
            return Result.success(orders);
        } catch (Exception e) {
            return Result.error("获取商家订单失败: " + e.getMessage());
        }
    }

    @Data
    public static class PayRequest {
        private String paymentMethod;
    }

    @Data
    public static class ShipRequest {
        private String logisticsCompany;
        private String trackingNumber;
    }

    @Data
    public static class LogisticsStatusRequest {
        private String status;
    }

    @GetMapping("/status-count")
    public Result<Map<String, Long>> getOrderStatusCount(HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return Result.error("未登录");
            }
            String token = authHeader.substring(7);
            String userId = jwtUtil.getUserIdFromToken(token);
            if (userId == null) {
                return Result.error("用户不存在");
            }
            Map<String, Long> counts = orderService.getOrderStatusCount(userId);
            return Result.success(counts);
        } catch (Exception e) {
            return Result.error("获取订单数量统计失败: " + e.getMessage());
        }
    }
}