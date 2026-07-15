package com.example.trendytoysocialecommercehd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.trendytoysocialecommercehd.dto.CreateOrderRequest;
import com.example.trendytoysocialecommercehd.dto.BatchCreateOrderRequest;
import com.example.trendytoysocialecommercehd.dto.OrderDetailDTO;
import com.example.trendytoysocialecommercehd.dto.OrderItemRequest;
import com.example.trendytoysocialecommercehd.dto.OrderListDTO;
import com.example.trendytoysocialecommercehd.entity.*;
import com.example.trendytoysocialecommercehd.mapper.*;
import lombok.Data;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private SaleVariantMapper saleVariantMapper;

    @Autowired
    private SaleSeriesMapper saleSeriesMapper;

    @Autowired
    private ShopMapper shopMapper;

    @Autowired
    private CouponService couponService;

    @Transactional(rollbackFor = Exception.class)
    public Order createOrder(CreateOrderRequest request) {
        String orderId = UUID.randomUUID().toString();
        String orderNo = generateOrderNo();

        int totalQuantity = request.getItems().stream()
                .mapToInt(OrderItemRequest::getQuantity)
                .sum();
        int productVarietyCount = request.getItems().size();

        // 优惠券处理：校验并重算折扣
        BigDecimal couponDiscount = BigDecimal.ZERO;
        if (request.getUserCouponId() != null && !request.getUserCouponId().isEmpty()) {
            couponDiscount = couponService.validateAndCalcDiscount(request.getUserCouponId(), request.getAmount());
        }

        Order order = new Order();
        order.setOrderId(orderId);
        order.setOrderNo(orderNo);
        order.setUserId(request.getUserId());
        order.setAmount(request.getAmount());
        order.setShippingFee(request.getShippingFee() != null ? request.getShippingFee() : BigDecimal.ZERO);
        BigDecimal totalDiscount = (request.getTotalDiscount() != null ? request.getTotalDiscount() : BigDecimal.ZERO).add(couponDiscount);
        order.setTotalDiscount(totalDiscount);
        BigDecimal actualAmount = request.getActualAmount().subtract(couponDiscount);
        if (actualAmount.compareTo(BigDecimal.ZERO) < 0) {
            actualAmount = BigDecimal.ZERO;
        }
        order.setActualAmount(actualAmount);
        order.setSettlementAmount(actualAmount);
        order.setPlatformCommission(BigDecimal.ZERO);
        order.setRefundAmount(BigDecimal.ZERO);
        order.setOrderStatus("PENDING_PAYMENT");
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

            if (itemRequest.getProductName() != null) {
                orderItem.setProductName(itemRequest.getProductName());
            }
            if (itemRequest.getProductImage() != null) {
                orderItem.setProductImage(itemRequest.getProductImage());
            }
            if (itemRequest.getProductSpec() != null) {
                orderItem.setProductSpec(itemRequest.getProductSpec());
            }

            if (orderItem.getProductName() == null) {
                SaleVariant saleVariant = saleVariantMapper.selectById(itemRequest.getProductId());
                if (saleVariant != null) {
                    orderItem.setProductName(saleVariant.getSkuCode());

                    if (saleVariant.getCustomImages() != null && !saleVariant.getCustomImages().isEmpty()) {
                        try {
                            String images = saleVariant.getCustomImages();
                            if (images.startsWith("[") && images.endsWith("]")) {
                                images = images.substring(1, images.length() - 1);
                                if (images.contains(",")) {
                                    String firstImage = images.split(",")[0].trim().replaceAll("\"", "");
                                    orderItem.setProductImage(firstImage);
                                } else {
                                    orderItem.setProductImage(images.trim().replaceAll("\"", ""));
                                }
                            }
                        } catch (Exception e) {
                        }
                    }

                    StringBuilder spec = new StringBuilder();
                    if (saleVariant.getSaleSeriesId() != null) {
                        SaleSeries saleSeries = saleSeriesMapper.selectById(saleVariant.getSaleSeriesId());
                        if (saleSeries != null && saleSeries.getSaleTitle() != null) {
                            spec.append(saleSeries.getSaleTitle());
                        }
                    }
                    if (saleVariant.getCustomDescription() != null) {
                        if (spec.length() > 0) {
                            spec.append(" - ");
                        }
                        String desc = saleVariant.getCustomDescription();
                        if (desc.length() > 20) {
                            desc = desc.substring(0, 20) + "...";
                        }
                        spec.append(desc);
                    }
                    orderItem.setProductSpec(spec.toString());
                }
            }

            orderItem.setCreateTime(LocalDateTime.now());
            orderItem.setUpdateTime(LocalDateTime.now());

            orderItemMapper.insert(orderItem);
        }

        // 标记优惠券为已使用
        if (request.getUserCouponId() != null && !request.getUserCouponId().isEmpty()) {
            couponService.useCoupon(request.getUserCouponId(), orderId);
        }

        return order;
    }

    @Transactional(rollbackFor = Exception.class)
    public List<Order> batchCreateOrders(BatchCreateOrderRequest request) {
        List<Order> orders = new ArrayList<>();

        // 优惠券处理：校验并计算总扣减金额（基于所有店铺订单总额）
        BigDecimal couponDiscount = BigDecimal.ZERO;
        boolean hasCoupon = request.getUserCouponId() != null && !request.getUserCouponId().isEmpty();
        if (hasCoupon) {
            BigDecimal totalAmount = BigDecimal.ZERO;
            for (BatchCreateOrderRequest.ShopOrderRequest shopOrder : request.getShopOrders()) {
                totalAmount = totalAmount.add(shopOrder.getAmount());
            }
            couponDiscount = couponService.validateAndCalcDiscount(request.getUserCouponId(), totalAmount);
        }

        // 用于记录第一个订单ID，将券绑定到该订单（取主单）
        String firstOrderId = null;
        int shopIndex = 0;
        BigDecimal allocatedDiscountSum = BigDecimal.ZERO;

        for (BatchCreateOrderRequest.ShopOrderRequest shopOrder : request.getShopOrders()) {
            String orderId = UUID.randomUUID().toString();
            String orderNo = generateOrderNo();
            if (firstOrderId == null) {
                firstOrderId = orderId;
            }

            int totalQuantity = shopOrder.getItems().stream()
                    .mapToInt(OrderItemRequest::getQuantity)
                    .sum();
            int productVarietyCount = shopOrder.getItems().size();

            // 按比例分摊券折扣到最后一个订单承担尾差
            BigDecimal shopCouponDiscount = BigDecimal.ZERO;
            if (hasCoupon && couponDiscount.compareTo(BigDecimal.ZERO) > 0) {
                if (shopIndex == request.getShopOrders().size() - 1) {
                    // 最后一个订单承担剩余
                    shopCouponDiscount = couponDiscount.subtract(allocatedDiscountSum);
                    if (shopCouponDiscount.compareTo(BigDecimal.ZERO) < 0) {
                        shopCouponDiscount = BigDecimal.ZERO;
                    }
                } else {
                    // 按店铺金额占比分摊
                    BigDecimal totalAmount = BigDecimal.ZERO;
                    for (BatchCreateOrderRequest.ShopOrderRequest so : request.getShopOrders()) {
                        totalAmount = totalAmount.add(so.getAmount());
                    }
                    if (totalAmount.compareTo(BigDecimal.ZERO) > 0) {
                        shopCouponDiscount = couponDiscount.multiply(shopOrder.getAmount())
                                .divide(totalAmount, 2, java.math.RoundingMode.HALF_UP);
                    }
                    allocatedDiscountSum = allocatedDiscountSum.add(shopCouponDiscount);
                }
            }

            Order order = new Order();
            order.setOrderId(orderId);
            order.setOrderNo(orderNo);
            order.setUserId(request.getUserId());
            order.setAmount(shopOrder.getAmount());
            order.setShippingFee(shopOrder.getShippingFee() != null ? shopOrder.getShippingFee() : BigDecimal.ZERO);
            BigDecimal totalDiscount = (shopOrder.getTotalDiscount() != null ? shopOrder.getTotalDiscount() : BigDecimal.ZERO).add(shopCouponDiscount);
            order.setTotalDiscount(totalDiscount);
            BigDecimal actualAmount = shopOrder.getActualAmount().subtract(shopCouponDiscount);
            if (actualAmount.compareTo(BigDecimal.ZERO) < 0) {
                actualAmount = BigDecimal.ZERO;
            }
            order.setActualAmount(actualAmount);
            order.setSettlementAmount(actualAmount);
            order.setPlatformCommission(BigDecimal.ZERO);
            order.setRefundAmount(BigDecimal.ZERO);
            order.setOrderStatus("PENDING_PAYMENT");
            order.setAfterSalesStatus("NONE");
            order.setTotalQuantity(totalQuantity);
            order.setProductVarietyCount(productVarietyCount);
            order.setAddressId(request.getAddressId());
            order.setUserRemark(request.getUserRemark());
            order.setPaymentDeadline(LocalDateTime.now().plusMinutes(30));
            order.setCreateTime(LocalDateTime.now());
            order.setUpdateTime(LocalDateTime.now());

            orderMapper.insert(order);
            orders.add(order);

            for (OrderItemRequest itemRequest : shopOrder.getItems()) {
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
                orderItem.setItemSellerId(shopOrder.getShopId());

                if (itemRequest.getProductName() != null) {
                    orderItem.setProductName(itemRequest.getProductName());
                }
                if (itemRequest.getProductImage() != null) {
                    orderItem.setProductImage(itemRequest.getProductImage());
                }
                if (itemRequest.getProductSpec() != null) {
                    orderItem.setProductSpec(itemRequest.getProductSpec());
                }

                if (orderItem.getProductName() == null) {
                    SaleVariant saleVariant = saleVariantMapper.selectById(itemRequest.getProductId());
                    if (saleVariant != null) {
                        orderItem.setProductName(saleVariant.getSkuCode());

                        if (saleVariant.getCustomImages() != null && !saleVariant.getCustomImages().isEmpty()) {
                            try {
                                String images = saleVariant.getCustomImages();
                                if (images.startsWith("[") && images.endsWith("]")) {
                                    images = images.substring(1, images.length() - 1);
                                    if (images.contains(",")) {
                                        String firstImage = images.split(",")[0].trim().replaceAll("\"", "");
                                        orderItem.setProductImage(firstImage);
                                    } else {
                                        orderItem.setProductImage(images.trim().replaceAll("\"", ""));
                                    }
                                }
                            } catch (Exception e) {
                            }
                        }

                        StringBuilder spec = new StringBuilder();
                        if (saleVariant.getSaleSeriesId() != null) {
                            SaleSeries saleSeries = saleSeriesMapper.selectById(saleVariant.getSaleSeriesId());
                            if (saleSeries != null && saleSeries.getSaleTitle() != null) {
                                spec.append(saleSeries.getSaleTitle());
                            }
                        }
                        if (saleVariant.getCustomDescription() != null) {
                            if (spec.length() > 0) spec.append(" - ");
                            String desc = saleVariant.getCustomDescription();
                            if (desc.length() > 20) desc = desc.substring(0, 20) + "...";
                            spec.append(desc);
                        }
                        orderItem.setProductSpec(spec.toString());
                    }
                }

                orderItem.setCreateTime(LocalDateTime.now());
                orderItem.setUpdateTime(LocalDateTime.now());

                orderItemMapper.insert(orderItem);
            }
            shopIndex++;
        }

        // 标记优惠券为已使用（绑定到主单）
        if (hasCoupon) {
            couponService.useCoupon(request.getUserCouponId(), firstOrderId);
        }

        return orders;
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

    public List<OrderListDTO> getSellerOrdersWithItems(String sellerId, String status) {
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
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Order::getOrderStatus, status);
        }
        wrapper.orderByDesc(Order::getCreateTime);
        List<Order> orders = orderMapper.selectList(wrapper);

        List<OrderListDTO> result = new ArrayList<>();
        for (Order order : orders) {
            OrderListDTO dto = new OrderListDTO();
            BeanUtils.copyProperties(order, dto);

            List<OrderItem> items = getOrderItemsByOrderId(order.getOrderId());

            for (OrderItem item : items) {
                if (item.getProductName() == null || item.getProductImage() == null || item.getProductSpec() == null) {
                    SaleVariant saleVariant = saleVariantMapper.selectById(item.getProductId());
                    if (saleVariant != null) {
                        if (item.getProductName() == null) {
                            item.setProductName(saleVariant.getSkuCode());
                        }
                        if (item.getProductImage() == null && saleVariant.getCustomImages() != null) {
                            try {
                                String images = saleVariant.getCustomImages();
                                if (images.startsWith("[") && images.endsWith("]")) {
                                    images = images.substring(1, images.length() - 1);
                                    if (images.contains(",")) {
                                        String firstImage = images.split(",")[0].trim().replaceAll("\"", "");
                                        item.setProductImage(firstImage);
                                    } else {
                                        item.setProductImage(images.trim().replaceAll("\"", ""));
                                    }
                                }
                            } catch (Exception e) {
                            }
                        }
                        if (item.getProductSpec() == null) {
                            StringBuilder spec = new StringBuilder();
                            if (saleVariant.getSaleSeriesId() != null) {
                                SaleSeries saleSeries = saleSeriesMapper.selectById(saleVariant.getSaleSeriesId());
                                if (saleSeries != null && saleSeries.getSaleTitle() != null) {
                                    spec.append(saleSeries.getSaleTitle());
                                }
                            }
                            if (saleVariant.getCustomDescription() != null) {
                                if (spec.length() > 0) spec.append(" - ");
                                String desc = saleVariant.getCustomDescription();
                                if (desc.length() > 20) desc = desc.substring(0, 20) + "...";
                                spec.append(desc);
                            }
                            item.setProductSpec(spec.toString());
                        }
                    }
                }
            }

            if (!items.isEmpty()) {
                OrderItem firstItem = items.get(0);
                String shopId = firstItem.getItemSellerId();
                if (shopId != null) {
                    Shop shop = shopMapper.selectById(shopId);
                    if (shop != null) {
                        dto.setShopName(shop.getShopName());
                    }
                }
            }

            if (dto.getShopName() == null) {
                dto.setShopName("店铺");
            }

            dto.setItems(items);
            result.add(dto);
        }

        return result;
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
        BeanUtils.copyProperties(order, orderDetail);

        List<OrderItem> orderItems = getOrderItemsByOrderId(orderId);

        for (OrderItem item : orderItems) {
            if (item.getProductName() == null || item.getProductImage() == null || item.getProductSpec() == null) {
                SaleVariant saleVariant = saleVariantMapper.selectById(item.getProductId());
                if (saleVariant != null) {
                    if (item.getProductName() == null) {
                        item.setProductName(saleVariant.getSkuCode());
                    }
                    if (item.getProductImage() == null && saleVariant.getCustomImages() != null) {
                        try {
                            String images = saleVariant.getCustomImages();
                            if (images.startsWith("[") && images.endsWith("]")) {
                                images = images.substring(1, images.length() - 1);
                                if (images.contains(",")) {
                                    String firstImage = images.split(",")[0].trim().replaceAll("\"", "");
                                    item.setProductImage(firstImage);
                                } else {
                                    item.setProductImage(images.trim().replaceAll("\"", ""));
                                }
                            }
                        } catch (Exception e) {
                        }
                    }
                    if (item.getProductSpec() == null) {
                        StringBuilder spec = new StringBuilder();
                        if (saleVariant.getSaleSeriesId() != null) {
                            SaleSeries saleSeries = saleSeriesMapper.selectById(saleVariant.getSaleSeriesId());
                            if (saleSeries != null && saleSeries.getSaleTitle() != null) {
                                spec.append(saleSeries.getSaleTitle());
                            }
                        }
                        if (saleVariant.getCustomDescription() != null) {
                            if (spec.length() > 0) spec.append(" - ");
                            String desc = saleVariant.getCustomDescription();
                            if (desc.length() > 20) desc = desc.substring(0, 20) + "...";
                            spec.append(desc);
                        }
                        item.setProductSpec(spec.toString());
                    }
                }
            }
        }

        orderDetail.setOrderItems(orderItems);

        return orderDetail;
    }

    @Transactional(rollbackFor = Exception.class)
    public Order payOrder(String orderId, String paymentMethod) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        if (!"PENDING_PAYMENT".equals(order.getOrderStatus())) {
            throw new RuntimeException("订单状态不正确，无法支付");
        }

        order.setOrderStatus("PENDING_SHIPMENT");
        order.setPaymentMethod(paymentMethod);
        order.setPaymentTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());

        orderMapper.updateById(order);
        return order;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean deleteOrder(String orderId) {
        LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(OrderItem::getOrderId, orderId);
        orderItemMapper.delete(itemWrapper);

        int result = orderMapper.deleteById(orderId);
        return result > 0;
    }

    public List<OrderListDTO> getUserOrdersWithItems(String userId, String status) {
        List<Order> orders = getOrdersByUserId(userId, status);
        List<OrderListDTO> result = new ArrayList<>();

        for (Order order : orders) {
            OrderListDTO dto = new OrderListDTO();
            org.springframework.beans.BeanUtils.copyProperties(order, dto);

            List<OrderItem> items = getOrderItemsByOrderId(order.getOrderId());

            for (OrderItem item : items) {
                if (item.getProductName() == null || item.getProductImage() == null) {
                    SaleVariant saleVariant = saleVariantMapper.selectById(item.getProductId());
                    if (saleVariant != null) {
                        if (item.getProductName() == null) {
                            item.setProductName(saleVariant.getSkuCode());
                        }
                        if (item.getProductImage() == null && saleVariant.getCustomImages() != null) {
                            try {
                                String images = saleVariant.getCustomImages();
                                if (images.startsWith("[") && images.endsWith("]")) {
                                    images = images.substring(1, images.length() - 1);
                                    if (images.contains(",")) {
                                        String firstImage = images.split(",")[0].trim().replaceAll("\"", "");
                                        item.setProductImage(firstImage);
                                    } else {
                                        item.setProductImage(images.trim().replaceAll("\"", ""));
                                    }
                                }
                            } catch (Exception e) {
                            }
                        }
                        if (item.getProductSpec() == null) {
                            StringBuilder spec = new StringBuilder();
                            if (saleVariant.getSaleSeriesId() != null) {
                                SaleSeries saleSeries = saleSeriesMapper.selectById(saleVariant.getSaleSeriesId());
                                if (saleSeries != null && saleSeries.getSaleTitle() != null) {
                                    spec.append(saleSeries.getSaleTitle());
                                }
                            }
                            if (saleVariant.getCustomDescription() != null) {
                                if (spec.length() > 0) spec.append(" - ");
                                String desc = saleVariant.getCustomDescription();
                                if (desc.length() > 20) desc = desc.substring(0, 20) + "...";
                                spec.append(desc);
                            }
                            item.setProductSpec(spec.toString());
                        }
                    }
                }
            }

            if (!items.isEmpty()) {
                OrderItem firstItem = items.get(0);
                String shopId = firstItem.getItemSellerId();
                if (shopId != null) {
                    Shop shop = shopMapper.selectById(shopId);
                    if (shop != null) {
                        dto.setShopName(shop.getShopName());
                    }
                }
            }

            if (dto.getShopName() == null) {
                dto.setShopName("店铺");
            }

            dto.setItems(items);
            result.add(dto);
        }

        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public Order cancelOrder(String orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        if (!"PENDING_PAYMENT".equals(order.getOrderStatus())) {
            throw new RuntimeException("订单状态不正确，无法取消");
        }

        order.setOrderStatus("CANCELLED");
        order.setCancelTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());

        orderMapper.updateById(order);

        // 恢复该订单使用的优惠券（若有）
        couponService.restoreCouponByOrderId(orderId);

        return order;
    }

    @Transactional(rollbackFor = Exception.class)
    public Order shipOrder(String orderId, String logisticsCompany, String trackingNumber) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        if (!"PENDING_SHIPMENT".equals(order.getOrderStatus())) {
            throw new RuntimeException("订单状态不正确，无法发货");
        }

        order.setLogisticsCompany(logisticsCompany);
        order.setTrackingNumber(trackingNumber);
        order.setShippedTime(LocalDateTime.now());
        order.setOrderStatus("SHIPPED");
        order.setUpdateTime(LocalDateTime.now());

        orderMapper.updateById(order);
        return order;
    }

    @Transactional(rollbackFor = Exception.class)
    public Order completeOrder(String orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        if (!"SHIPPED".equals(order.getOrderStatus())) {
            throw new RuntimeException("订单状态不正确，无法完成");
        }

        order.setReceivedTime(LocalDateTime.now());
        order.setCompleteTime(LocalDateTime.now());
        order.setOrderStatus("COMPLETED");
        order.setUpdateTime(LocalDateTime.now());

        orderMapper.updateById(order);
        return order;
    }

    public LogisticsInfo getLogisticsInfo(String orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        LogisticsInfo logisticsInfo = new LogisticsInfo();
        logisticsInfo.setOrderId(orderId);
        logisticsInfo.setLogisticsCompany(order.getLogisticsCompany());
        logisticsInfo.setTrackingNumber(order.getTrackingNumber());

        List<LogisticsTrack> tracks = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        tracks.add(createTrack(now, "signed", "已签收"));
        tracks.add(createTrack(now.minusHours(6), "delivering", "派件中"));
        tracks.add(createTrack(now.minusDays(1), "in_transit", "运输中"));
        tracks.add(createTrack(now.minusDays(1).minusHours(6), "collected", "已揽收"));

        logisticsInfo.setTracks(tracks);
        return logisticsInfo;
    }

    private LogisticsTrack createTrack(LocalDateTime time, String status, String description) {
        LogisticsTrack track = new LogisticsTrack();
        track.setTime(time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        track.setStatus(status);
        track.setDescription(description);
        return track;
    }

    public Map<String, Long> getOrderStatusCount(String userId) {
        Map<String, Long> counts = new HashMap<>();
        counts.put("unpaid", countByStatus(userId, "PENDING_PAYMENT"));
        counts.put("unshipped", countByStatus(userId, "PENDING_SHIPMENT"));
        counts.put("shipping", countByStatus(userId, "SHIPPED"));
        counts.put("completed", countByStatus(userId, "COMPLETED"));
        return counts;
    }

    private long countByStatus(String userId, String status) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getUserId, userId);
        wrapper.eq(Order::getOrderStatus, status);
        return orderMapper.selectCount(wrapper);
    }

    @Data
    public static class LogisticsInfo {
        private String orderId;
        private String logisticsCompany;
        private String trackingNumber;
        private List<LogisticsTrack> tracks;
    }

    @Data
    public static class LogisticsTrack {
        private String time;
        private String status;
        private String description;
    }
}