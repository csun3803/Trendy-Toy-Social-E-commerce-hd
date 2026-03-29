package com.example.trendytoysocialecommercehd.controller;

import com.example.trendytoysocialecommercehd.common.Result;
import com.example.trendytoysocialecommercehd.dto.CreateOrderRequest;
import com.example.trendytoysocialecommercehd.dto.OrderDetailDTO;
import com.example.trendytoysocialecommercehd.entity.Order;
import com.example.trendytoysocialecommercehd.service.OrderService;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping
    public Result<Order> createOrder(@RequestBody CreateOrderRequest request) {
        try {
            Order order = orderService.createOrder(request);
            return Result.success(order);
        } catch (Exception e) {
            return Result.error("创建订单失败: " + e.getMessage());
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

    @GetMapping("/user/{userId}")
    public Result<List<Order>> getUserOrders(
            @PathVariable String userId,
            @RequestParam(required = false) String status) {
        try {
            List<Order> orders = orderService.getOrdersByUserId(userId, status);
            return Result.success(orders);
        } catch (Exception e) {
            return Result.error("获取订单列表失败: " + e.getMessage());
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
            return Result.error("支付失败：" + e.getMessage());
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
}