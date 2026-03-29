package com.example.trendytoysocialecommercehd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.trendytoysocialecommercehd.dto.CreateOrderRequest;
import com.example.trendytoysocialecommercehd.dto.OrderDetailDTO;
import com.example.trendytoysocialecommercehd.dto.OrderItemRequest;
import com.example.trendytoysocialecommercehd.entity.Order;
import com.example.trendytoysocialecommercehd.entity.OrderItem;
import com.example.trendytoysocialecommercehd.mapper.OrderItemMapper;
import com.example.trendytoysocialecommercehd.mapper.OrderMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Transactional(rollbackFor = Exception.class)
    public Order createOrder(CreateOrderRequest request) {
        String orderId = UUID.randomUUID().toString();
        String orderNo = generateOrderNo();

        int totalQuantity = request.getItems().stream()
                .mapToInt(OrderItemRequest::getQuantity)
                .sum();
        int productVarietyCount = request.getItems().size();

        Order order = new Order();
        order.setOrderId(orderId);
        order.setOrderNo(orderNo);
        order.setUserId(request.getUserId());
        order.setAmount(request.getAmount());
        order.setShippingFee(request.getShippingFee() != null ? request.getShippingFee() : BigDecimal.ZERO);
        order.setTotalDiscount(request.getTotalDiscount() != null ? request.getTotalDiscount() : BigDecimal.ZERO);
        order.setActualAmount(request.getActualAmount());
        order.setSettlementAmount(request.getActualAmount());
        order.setPlatformCommission(BigDecimal.ZERO);
        order.setRefundAmount(BigDecimal.ZERO);
        order.setPaymentStatus("UNPAID");
        order.setOrderStatus("PENDING");
        order.setShippingStatus("UNSHIPPED");
        order.setAfterSalesStatus("NONE");
        order.setTotalQuantity(totalQuantity);
        order.setProductVarietyCount(productVarietyCount);
        order.setAddressId(request.getAddressId());
        order.setUserRemark(request.getUserRemark());
        order.setPaymentDeadline(LocalDateTime.now().plusMinutes(30));
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());

        orderMapper.insert(order);

        for (OrderItemRequest itemRequest : request.getItems()) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrderItemId(UUID.randomUUID().toString());
            orderItem.setOrderId(orderId);
            orderItem.setProductId(itemRequest.getProductId());
            orderItem.setOriginalPrice(itemRequest.getOriginalPrice());
            orderItem.setUnitPrice(itemRequest.getUnitPrice());
            orderItem.setQuantity(itemRequest.getQuantity());
            orderItem.setSubtotalAmount(itemRequest.getSubtotalAmount());
            orderItem.setAllocatedDiscount(itemRequest.getAllocatedDiscount() != null ? itemRequest.getAllocatedDiscount() : BigDecimal.ZERO);
            orderItem.setActualSubtotal(itemRequest.getActualSubtotal());
            orderItem.setItemAfterSalesStatus("NONE");
            orderItem.setItemRefundAmount(BigDecimal.ZERO);
            orderItem.setRefundQuantity(0);
            orderItem.setItemSellerId(itemRequest.getItemSellerId());
            orderItem.setCreateTime(LocalDateTime.now());
            orderItem.setUpdateTime(LocalDateTime.now());

            orderItemMapper.insert(orderItem);
        }

        return order;
    }

    public Order getOrderById(String orderId) {
        return orderMapper.selectById(orderId);
    }

    public List<Order> getOrdersByUserId(String userId, String status) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getUserId, userId);

        if (status != null && !status.isEmpty()) {
            wrapper.eq(Order::getOrderStatus, status);
        }

        wrapper.orderByDesc(Order::getCreateTime);
        return orderMapper.selectList(wrapper);
    }

    private String generateOrderNo() {
        return "ORD" + System.currentTimeMillis() + (int)(Math.random() * 1000);
    }

    public List<Order> getOrdersBySellerId(String sellerId) {
        LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(OrderItem::getItemSellerId, sellerId);
        itemWrapper.select(OrderItem::getOrderId);
        itemWrapper.groupBy(OrderItem::getOrderId);

        List<OrderItem> orderItems = orderItemMapper.selectList(itemWrapper);
        List<String> orderIds = orderItems.stream()
                .map(OrderItem::getOrderId)
                .distinct()
                .collect(java.util.stream.Collectors.toList());

        if (orderIds.isEmpty()) {
            return Collections.emptyList();
        }

        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Order::getOrderId, orderIds);
        wrapper.orderByDesc(Order::getCreateTime);
        return orderMapper.selectList(wrapper);
    }

    public List<OrderItem> getOrderItemsByOrderId(String orderId) {
        LambdaQueryWrapper<OrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderItem::getOrderId, orderId);
        return orderItemMapper.selectList(wrapper);
    }


    public OrderDetailDTO getOrderDetail(String orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            return null;
        }

        OrderDetailDTO orderDetail = new OrderDetailDTO();
        // 复制Order的所有字段到OrderDetail
        BeanUtils.copyProperties(order, orderDetail);

        // 查询订单商品
        List<OrderItem> orderItems = getOrderItemsByOrderId(orderId);
        orderDetail.setOrderItems(orderItems);

        return orderDetail;
    }

    @Transactional(rollbackFor = Exception.class)
    public Order payOrder(String orderId, String paymentMethod) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        if (!"PENDING".equals(order.getOrderStatus())) {
            throw new RuntimeException("订单状态不正确，无法支付");
        }

        order.setOrderStatus("PENDING_SHIPMENT");
        order.setPaymentStatus("PAID");
        order.setPaymentMethod(paymentMethod);
        order.setPaymentTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());

        orderMapper.updateById(order);
        return order;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean deleteOrder(String orderId) {
        // 先删除订单详情
        LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(OrderItem::getOrderId, orderId);
        orderItemMapper.delete(itemWrapper);

        // 再删除订单
        int result = orderMapper.deleteById(orderId);
        return result > 0;
    }

}