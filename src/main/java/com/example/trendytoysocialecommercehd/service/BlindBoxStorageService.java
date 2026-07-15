package com.example.trendytoysocialecommercehd.service;

import com.example.trendytoysocialecommercehd.entity.*;
import com.example.trendytoysocialecommercehd.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

/**
 * 盲盒暂存柜 Service
 */
@Service
public class BlindBoxStorageService {

    @Autowired
    private BlindBoxStorageMapper blindBoxStorageMapper;

    @Autowired
    private BlindBoxMachineMapper blindBoxMachineMapper;

    @Autowired
    private SaleVariantMapper saleVariantMapper;

    @Autowired
    private SaleSeriesMapper saleSeriesMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    /**
     * 存入暂存柜（抽中后调用）
     */
    public BlindBoxStorage storeToCabinet(String userId, String machineId, String setId,
                                          Integer slotNo, String saleVariantId, Boolean isHidden,
                                          BigDecimal drawPrice, String payOrderId) {
        BlindBoxMachine machine = blindBoxMachineMapper.selectById(machineId);
        SaleVariant variant = saleVariantMapper.selectById(saleVariantId);

        BlindBoxStorage storage = new BlindBoxStorage();
        storage.setStorageId(UUID.randomUUID().toString());
        storage.setUserId(userId);
        storage.setMachineId(machineId);
        storage.setMachineName(machine != null ? machine.getMachineName() : null);
        storage.setSetId(setId);
        storage.setSlotNo(slotNo);
        storage.setSaleVariantId(saleVariantId);
        storage.setVariantId(variant != null ? variant.getVariantId() : null);
        storage.setVariantName(variant != null ? (variant.getCustomDescription() != null
                ? variant.getCustomDescription() : variant.getSkuCode()) : null);
        storage.setVariantImage(variant != null ? parseVariantImage(variant.getCustomImages()) : null);
        storage.setIsHidden(isHidden);
        storage.setDrawPrice(drawPrice);
        storage.setPayOrderId(payOrderId);
        storage.setStatus("STORED");
        storage.setStoredAt(new Date());
        blindBoxStorageMapper.insert(storage);
        return storage;
    }

    /**
     * 获取用户暂存柜列表
     */
    public List<BlindBoxStorage> getUserStorage(String userId, boolean onlyStored) {
        if (onlyStored) {
            return blindBoxStorageMapper.selectStoredByUserId(userId);
        }
        return blindBoxStorageMapper.selectAllByUserId(userId);
    }

    /**
     * 获取暂存数量
     */
    public int getStoredCount(String userId) {
        return blindBoxStorageMapper.countStoredByUserId(userId);
    }

    /**
     * 发货：将暂存柜记录关联的抽盒订单更新为待发货
     */
    @Transactional(rollbackFor = Exception.class)
    public Order shipFromCabinet(String storageId, String userId, String addressId) {
        BlindBoxStorage storage = blindBoxStorageMapper.selectById(storageId);
        if (storage == null) {
            throw new RuntimeException("暂存柜记录不存在");
        }
        if (!"STORED".equals(storage.getStatus())) {
            throw new RuntimeException("该盲盒已发货");
        }
        if (!storage.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作他人暂存柜");
        }

        // 查找抽盒时创建的订单（payOrderId）
        String payOrderId = storage.getPayOrderId();
        if (payOrderId == null || payOrderId.isEmpty()) {
            throw new RuntimeException("未找到关联的抽盒订单");
        }

        Order order = orderMapper.selectById(payOrderId);
        if (order == null) {
            throw new RuntimeException("关联订单不存在");
        }

        // 将订单从待付款更新为待发货（盲盒已付费）
        order.setOrderStatus("PENDING_SHIPMENT");
        order.setPaymentMethod("BLIND_BOX_PAID");
        order.setPaymentTime(java.time.LocalDateTime.now());
        order.setAddressId(addressId);
        order.setUserRemark("盲盒暂存柜发货");
        order.setPaymentDeadline(null); // 已支付，清除付款截止时间
        order.setUpdateTime(java.time.LocalDateTime.now());
        orderMapper.updateById(order);

        // 补充订单项的店铺信息（如之前缺失）
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OrderItem> itemQuery =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        itemQuery.eq(OrderItem::getOrderId, payOrderId);
        List<OrderItem> items = orderItemMapper.selectList(itemQuery);
        if (!items.isEmpty()) {
            OrderItem item = items.get(0);
            boolean needUpdate = false;

            // 补充店铺ID
            if (item.getItemSellerId() == null || item.getItemSellerId().isEmpty()) {
                BlindBoxMachine machine = blindBoxMachineMapper.selectById(storage.getMachineId());
                String shopId = machine != null ? machine.getShopId() : null;
                if (shopId == null) {
                    SaleVariant variant = saleVariantMapper.selectById(storage.getSaleVariantId());
                    if (variant != null && variant.getSaleSeriesId() != null) {
                        SaleSeries series = saleSeriesMapper.selectById(variant.getSaleSeriesId());
                        if (series != null) shopId = series.getShopId();
                    }
                }
                if (shopId != null) {
                    item.setItemSellerId(shopId);
                    needUpdate = true;
                }
            }

            // 补充商品名称
            if (item.getProductName() == null || item.getProductName().isEmpty()) {
                String name = storage.getVariantName();
                if (name != null) {
                    item.setProductName(name);
                    needUpdate = true;
                }
            }

            // 补充商品图片
            if (item.getProductImage() == null || item.getProductImage().isEmpty()) {
                String img = storage.getVariantImage();
                if (img != null) {
                    item.setProductImage(img);
                    needUpdate = true;
                }
            }

            if (needUpdate) {
                item.setUpdateTime(java.time.LocalDateTime.now());
                orderItemMapper.updateById(item);
            }
        }

        // 更新暂存柜状态
        storage.setStatus("SHIPPED");
        storage.setShipOrderId(order.getOrderId());
        storage.setShippedAt(new Date());
        blindBoxStorageMapper.updateById(storage);

        return order;
    }

    /**
     * 批量发货
     */
    @Transactional(rollbackFor = Exception.class)
    public List<Order> batchShip(List<String> storageIds, String userId, String addressId) {
        List<Order> orders = new ArrayList<>();
        for (String id : storageIds) {
            try {
                Order o = shipFromCabinet(id, userId, addressId);
                orders.add(o);
            } catch (Exception e) {
                // 跳过失败的
            }
        }
        return orders;
    }

    /** 解析款式图片（取第一张） */
    private String parseVariantImage(String customImages) {
        if (customImages == null || customImages.isEmpty()) return null;
        try {
            String[] arr = customImages.split(",");
            return arr.length > 0 ? arr[0].trim() : null;
        } catch (Exception e) {
            return customImages;
        }
    }
}
