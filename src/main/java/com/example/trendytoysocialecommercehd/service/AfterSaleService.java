package com.example.trendytoysocialecommercehd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.trendytoysocialecommercehd.dto.AfterSaleInfoDTO;
import com.example.trendytoysocialecommercehd.dto.CreateAfterSaleRequest;
import com.example.trendytoysocialecommercehd.entity.AfterSale;
import com.example.trendytoysocialecommercehd.entity.Order;
import com.example.trendytoysocialecommercehd.entity.OrderItem;
import com.example.trendytoysocialecommercehd.entity.Shop;
import com.example.trendytoysocialecommercehd.entity.User;
import com.example.trendytoysocialecommercehd.mapper.AfterSaleMapper;
import com.example.trendytoysocialecommercehd.mapper.OrderItemMapper;
import com.example.trendytoysocialecommercehd.mapper.OrderMapper;
import com.example.trendytoysocialecommercehd.mapper.ShopMapper;
import com.example.trendytoysocialecommercehd.mapper.UserMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Service
public class AfterSaleService {

    @Autowired
    private AfterSaleMapper afterSaleMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private ShopMapper shopMapper;

    @Autowired
    private UserMapper userMapper;

    // 商家审核超时时间（小时）：超时后自动同意
    private static final int MERCHANT_AUDIT_TIMEOUT_HOURS = 48;

    // 退货超时时间（天）：超过后自动关闭
    private static final int RETURN_TIMEOUT_DAYS = 7;

    @Transactional(rollbackFor = Exception.class)
    public AfterSale createAfterSale(CreateAfterSaleRequest request, String userId) {
        // 检查订单和订单项
        Order order = orderMapper.selectById(request.getOrderId());
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        OrderItem orderItem = orderItemMapper.selectById(request.getOrderItemId());
        if (orderItem == null) {
            throw new RuntimeException("订单项不存在");
        }

        if (!orderItem.getOrderId().equals(request.getOrderId())) {
            throw new RuntimeException("订单项不属于该订单");
        }

        // 检查是否已经有进行中的售后（包括平台介入审核中）
        LambdaQueryWrapper<AfterSale> existingWrapper = new LambdaQueryWrapper<>();
        existingWrapper.eq(AfterSale::getOrderItemId, request.getOrderItemId());
        existingWrapper.in(AfterSale::getAfterSaleStatus, "PENDING", "APPROVED", "PLATFORM_REVIEWING");
        List<AfterSale> existingAfterSales = afterSaleMapper.selectList(existingWrapper);
        if (!existingAfterSales.isEmpty()) {
            throw new RuntimeException("该商品已有进行中的售后申请");
        }

        // 创建售后记录
        AfterSale afterSale = new AfterSale();
        afterSale.setAfterSaleId(UUID.randomUUID().toString());
        afterSale.setOrderId(request.getOrderId());
        afterSale.setOrderItemId(request.getOrderItemId());
        afterSale.setUserId(order.getUserId());
        afterSale.setSellerId(orderItem.getItemSellerId());
        afterSale.setAfterSaleType(request.getAfterSaleType());
        afterSale.setAfterSaleStatus("PENDING");
        afterSale.setReason(request.getReason());
        afterSale.setDescription(request.getDescription());
        afterSale.setEvidenceImages(request.getEvidenceImages());
        afterSale.setRefundAmount(request.getRefundAmount());
        // 生成售后单号：AS + 时间戳 + 4位随机数
        String afterSaleNo = "AS" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%04d", new Random().nextInt(10000));
        afterSale.setAfterSaleNo(afterSaleNo);
        // 设置商家超时自动同意的截止时间
        afterSale.setTimeoutAutoApproveTime(LocalDateTime.now().plusHours(MERCHANT_AUDIT_TIMEOUT_HOURS));
        afterSale.setCreateTime(LocalDateTime.now());
        afterSale.setUpdateTime(LocalDateTime.now());

        afterSaleMapper.insert(afterSale);

        // 更新订单和订单项的售后状态
        order.setAfterSalesStatus("PROCESSING");
        order.setLastAfterSalesTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);

        orderItem.setItemAfterSalesStatus("PROCESSING");
        orderItem.setUpdateTime(LocalDateTime.now());
        orderItemMapper.updateById(orderItem);

        return afterSale;
    }

    @Transactional(rollbackFor = Exception.class)
    public AfterSale approveAfterSale(String afterSaleId) {
        AfterSale afterSale = afterSaleMapper.selectById(afterSaleId);
        if (afterSale == null) {
            throw new RuntimeException("售后申请不存在");
        }

        if (!"PENDING".equals(afterSale.getAfterSaleStatus())) {
            throw new RuntimeException("售后申请状态不正确，无法审核");
        }

        afterSale.setAfterSaleStatus("APPROVED");
        afterSale.setAuditTime(LocalDateTime.now());
        afterSale.setUpdateTime(LocalDateTime.now());

        // 如果是退货类型，设置退货截止时间（7天）并自动填充商家退货地址
        if ("RETURN".equals(afterSale.getAfterSaleType())) {
            afterSale.setReturnDeadline(LocalDateTime.now().plusDays(RETURN_TIMEOUT_DAYS));
            
            // 自动从商家信息获取退货地址
            Shop shop = shopMapper.selectById(afterSale.getSellerId());
            if (shop != null && shop.getReturnAddressDetail() != null) {
                // 拼接完整退货地址
                StringBuilder addressBuilder = new StringBuilder();
                if (shop.getReturnAddressContact() != null) {
                    addressBuilder.append("收货人：").append(shop.getReturnAddressContact()).append("\n");
                }
                if (shop.getReturnAddressPhone() != null) {
                    addressBuilder.append("电话：").append(shop.getReturnAddressPhone()).append("\n");
                }
                if (shop.getReturnAddressProvince() != null) {
                    addressBuilder.append(shop.getReturnAddressProvince());
                }
                if (shop.getReturnAddressCity() != null) {
                    addressBuilder.append(shop.getReturnAddressCity());
                }
                if (shop.getReturnAddressDistrict() != null) {
                    addressBuilder.append(shop.getReturnAddressDistrict());
                }
                if (shop.getReturnAddressDetail() != null) {
                    addressBuilder.append(shop.getReturnAddressDetail());
                }
                afterSale.setReturnAddress(addressBuilder.toString());
            }
        }

        afterSaleMapper.updateById(afterSale);

        // 如果是仅退款，直接完成
        if ("REFUND".equals(afterSale.getAfterSaleType())) {
            return completeAfterSaleInternal(afterSaleId);
        }

        return afterSale;
    }

    @Transactional(rollbackFor = Exception.class)
    public AfterSale rejectAfterSale(String afterSaleId, String rejectReason) {
        AfterSale afterSale = afterSaleMapper.selectById(afterSaleId);
        if (afterSale == null) {
            throw new RuntimeException("售后申请不存在");
        }

        if (!"PENDING".equals(afterSale.getAfterSaleStatus())) {
            throw new RuntimeException("售后申请状态不正确，无法审核");
        }

        afterSale.setAfterSaleStatus("REJECTED");
        afterSale.setRejectReason(rejectReason);
        afterSale.setAuditTime(LocalDateTime.now());
        afterSale.setUpdateTime(LocalDateTime.now());

        afterSaleMapper.updateById(afterSale);

        // 更新订单和订单项状态
        updateOrderAndItemStatus(afterSale.getOrderId(), afterSale.getOrderItemId());

        return afterSale;
    }

    /**
     * 商家填写退货地址（同意退货类售后后）
     */
    @Transactional(rollbackFor = Exception.class)
    public AfterSale fillReturnAddress(String afterSaleId, String returnAddress, String sellerId) {
        AfterSale afterSale = afterSaleMapper.selectById(afterSaleId);
        if (afterSale == null) {
            throw new RuntimeException("售后申请不存在");
        }

        if (!"APPROVED".equals(afterSale.getAfterSaleStatus())) {
            throw new RuntimeException("售后申请状态不正确，无法填写退货地址");
        }

        if (!"RETURN".equals(afterSale.getAfterSaleType())) {
            throw new RuntimeException("只有退货类型的售后才能填写退货地址");
        }

        if (sellerId != null && !sellerId.equals(afterSale.getSellerId())) {
            throw new RuntimeException("无权操作此售后申请");
        }

        afterSale.setReturnAddress(returnAddress);
        afterSale.setUpdateTime(LocalDateTime.now());
        afterSaleMapper.updateById(afterSale);

        return afterSale;
    }

    @Transactional(rollbackFor = Exception.class)
    public AfterSale submitReturnLogistics(String afterSaleId, String logisticsCompany, String trackingNumber) {
        AfterSale afterSale = afterSaleMapper.selectById(afterSaleId);
        if (afterSale == null) {
            throw new RuntimeException("售后申请不存在");
        }

        if (!"APPROVED".equals(afterSale.getAfterSaleStatus())) {
            throw new RuntimeException("售后申请状态不正确，无法提交物流");
        }

        if (!"RETURN".equals(afterSale.getAfterSaleType())) {
            throw new RuntimeException("只有退货类型的售后才能提交物流");
        }

        afterSale.setReturnLogisticsCompany(logisticsCompany);
        afterSale.setReturnTrackingNumber(trackingNumber);
        afterSale.setUpdateTime(LocalDateTime.now());

        afterSaleMapper.updateById(afterSale);

        return afterSale;
    }

    @Transactional(rollbackFor = Exception.class)
    public AfterSale confirmReturnReceived(String afterSaleId) {
        AfterSale afterSale = afterSaleMapper.selectById(afterSaleId);
        if (afterSale == null) {
            throw new RuntimeException("售后申请不存在");
        }

        if (!"APPROVED".equals(afterSale.getAfterSaleStatus())) {
            throw new RuntimeException("售后申请状态不正确，无法确认收货");
        }

        if (!"RETURN".equals(afterSale.getAfterSaleType())) {
            throw new RuntimeException("只有退货类型的售后才能确认收货");
        }

        if (afterSale.getReturnTrackingNumber() == null || afterSale.getReturnTrackingNumber().isEmpty()) {
            throw new RuntimeException("用户还未提交退货物流信息");
        }

        return completeAfterSaleInternal(afterSaleId);
    }

    /**
     * 用户申请平台介入（仅当商家拒绝后才能申请）
     */
    @Transactional(rollbackFor = Exception.class)
    public AfterSale applyPlatformIntervention(String afterSaleId, String reason, String userId) {
        AfterSale afterSale = afterSaleMapper.selectById(afterSaleId);
        if (afterSale == null) {
            throw new RuntimeException("售后申请不存在");
        }

        // 权限校验：只有发起售后的用户才能申请介入
        if (userId != null && !userId.equals(afterSale.getUserId())) {
            throw new RuntimeException("无权申请平台介入");
        }

        // 状态校验：只有商家拒绝后用户才能申请平台介入
        if (!"REJECTED".equals(afterSale.getAfterSaleStatus())) {
            throw new RuntimeException("只有商家拒绝售后申请后才能申请平台介入");
        }

        // 不能重复申请
        if ("PLATFORM_REVIEWING".equals(afterSale.getAfterSaleStatus())
                || "PLATFORM_RESOLVED".equals(afterSale.getAfterSaleStatus())) {
            throw new RuntimeException("已申请过平台介入，无法重复申请");
        }

        afterSale.setAfterSaleStatus("PLATFORM_REVIEWING");
        afterSale.setPlatformInterventionReason(reason);
        afterSale.setPlatformInterventionTime(LocalDateTime.now());
        afterSale.setUpdateTime(LocalDateTime.now());
        afterSaleMapper.updateById(afterSale);

        // 更新订单和订单项状态
        updateOrderAndItemStatus(afterSale.getOrderId(), afterSale.getOrderItemId());

        return afterSale;
    }

    /**
     * 平台管理员仲裁售后申请
     * @param result "USER" 表示支持用户（同意退款/退货），"SELLER" 表示支持商家（维持拒绝）
     */
    @Transactional(rollbackFor = Exception.class)
    public AfterSale arbitrateAfterSale(String afterSaleId, String result, String reason, String adminId) {
        AfterSale afterSale = afterSaleMapper.selectById(afterSaleId);
        if (afterSale == null) {
            throw new RuntimeException("售后申请不存在");
        }

        if (!"PLATFORM_REVIEWING".equals(afterSale.getAfterSaleStatus())) {
            throw new RuntimeException("售后申请不在平台介入审核中状态，无法仲裁");
        }

        if (!"USER".equals(result) && !"SELLER".equals(result)) {
            throw new RuntimeException("无效的裁决结果，必须为 USER 或 SELLER");
        }

        afterSale.setPlatformAdminId(adminId);
        afterSale.setPlatformArbitrationResult(result);
        afterSale.setPlatformArbitrationReason(reason);
        afterSale.setPlatformArbitrationTime(LocalDateTime.now());
        afterSale.setAfterSaleStatus("PLATFORM_RESOLVED");
        afterSale.setUpdateTime(LocalDateTime.now());
        afterSaleMapper.updateById(afterSale);

        // 根据裁决结果执行后续流程
        if ("USER".equals(result)) {
            // 支持用户：执行退款/退货流程
            if ("REFUND".equals(afterSale.getAfterSaleType())) {
                // 仅退款，直接完成
                completeAfterSaleInternal(afterSaleId);
            } else {
                // 退货类型：状态变为 APPROVED，等待用户寄回商品
                AfterSale refreshed = afterSaleMapper.selectById(afterSaleId);
                refreshed.setAfterSaleStatus("APPROVED");
                refreshed.setAuditTime(LocalDateTime.now());
                refreshed.setUpdateTime(LocalDateTime.now());
                afterSaleMapper.updateById(refreshed);
                // 注意：此时 platform_arbitration_* 字段保留，标识这是平台裁决后的退款流程
            }
        } else {
            // 支持商家：维持拒绝，更新订单状态
            updateOrderAndItemStatus(afterSale.getOrderId(), afterSale.getOrderItemId());
        }

        return afterSale;
    }

    /**
     * 定时任务：商家超时未处理自动同意
     * 每 10 分钟执行一次
     */
    @Scheduled(fixedRate = 10 * 60 * 1000)
    @Transactional(rollbackFor = Exception.class)
    public void processTimeoutAutoApprove() {
        LambdaQueryWrapper<AfterSale> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AfterSale::getAfterSaleStatus, "PENDING");
        wrapper.isNotNull(AfterSale::getTimeoutAutoApproveTime);
        wrapper.le(AfterSale::getTimeoutAutoApproveTime, LocalDateTime.now());

        List<AfterSale> timeoutList = afterSaleMapper.selectList(wrapper);
        for (AfterSale afterSale : timeoutList) {
            try {
                approveAfterSale(afterSale.getAfterSaleId());
            } catch (Exception e) {
                // 记录日志，不影响其他售后的处理
                System.err.println("自动同意售后失败: " + afterSale.getAfterSaleId() + ", 原因: " + e.getMessage());
            }
        }
    }

    /**
     * 定时任务：退货超时未寄回自动关闭
     * 每 10 分钟执行一次
     */
    @Scheduled(fixedRate = 10 * 60 * 1000)
    @Transactional(rollbackFor = Exception.class)
    public void processReturnTimeout() {
        LambdaQueryWrapper<AfterSale> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AfterSale::getAfterSaleStatus, "APPROVED");
        wrapper.eq(AfterSale::getAfterSaleType, "RETURN");
        wrapper.isNotNull(AfterSale::getReturnDeadline);
        wrapper.le(AfterSale::getReturnDeadline, LocalDateTime.now());

        List<AfterSale> timeoutList = afterSaleMapper.selectList(wrapper);
        for (AfterSale afterSale : timeoutList) {
            try {
                // 仅关闭未提交物流的售后
                if (afterSale.getReturnTrackingNumber() == null || afterSale.getReturnTrackingNumber().isEmpty()) {
                    closeAfterSaleInternal(afterSale.getAfterSaleId());
                }
            } catch (Exception e) {
                System.err.println("自动关闭退货超时售后失败: " + afterSale.getAfterSaleId() + ", 原因: " + e.getMessage());
            }
        }
    }

    /**
     * 用户取消售后申请（仅PENDING状态可取消）
     */
    @Transactional(rollbackFor = Exception.class)
    public AfterSale cancelAfterSale(String afterSaleId) {
        AfterSale afterSale = afterSaleMapper.selectById(afterSaleId);
        if (afterSale == null) {
            throw new RuntimeException("售后申请不存在");
        }

        if (!"PENDING".equals(afterSale.getAfterSaleStatus())) {
            throw new RuntimeException("只有等待商家处理的售后才能取消");
        }

        return closeAfterSaleInternal(afterSaleId);
    }

    /**
     * 内部方法：关闭售后申请
     */
    private AfterSale closeAfterSaleInternal(String afterSaleId) {
        AfterSale afterSale = afterSaleMapper.selectById(afterSaleId);
        if (afterSale == null) {
            throw new RuntimeException("售后申请不存在");
        }

        afterSale.setAfterSaleStatus("CLOSED");
        afterSale.setUpdateTime(LocalDateTime.now());
        afterSaleMapper.updateById(afterSale);

        // 更新订单和订单项状态
        updateOrderAndItemStatus(afterSale.getOrderId(), afterSale.getOrderItemId());

        return afterSale;
    }

    private AfterSale completeAfterSaleInternal(String afterSaleId) {
        AfterSale afterSale = afterSaleMapper.selectById(afterSaleId);
        if (afterSale == null) {
            throw new RuntimeException("售后申请不存在");
        }

        afterSale.setAfterSaleStatus("COMPLETED");
        afterSale.setCompleteTime(LocalDateTime.now());
        afterSale.setUpdateTime(LocalDateTime.now());

        afterSaleMapper.updateById(afterSale);

        // 更新订单和订单项的售后状态
        Order order = orderMapper.selectById(afterSale.getOrderId());
        OrderItem orderItem = orderItemMapper.selectById(afterSale.getOrderItemId());

        if (order != null) {
            order.setRefundAmount(order.getRefundAmount().add(afterSale.getRefundAmount()));
            order.setUpdateTime(LocalDateTime.now());
            orderMapper.updateById(order);
        }

        if (orderItem != null) {
            orderItem.setItemAfterSalesStatus("COMPLETED");
            orderItem.setItemRefundAmount(afterSale.getRefundAmount());
            orderItem.setRefundQuantity(orderItem.getQuantity());
            orderItem.setUpdateTime(LocalDateTime.now());
            orderItemMapper.updateById(orderItem);
        }

        // 检查订单下是否还有进行中的售后
        updateOrderAndItemStatus(afterSale.getOrderId(), afterSale.getOrderItemId());

        return afterSale;
    }

    private void updateOrderAndItemStatus(String orderId, String orderItemId) {
        // 检查该订单项是否还有进行中的售后（包括平台介入审核中）
        LambdaQueryWrapper<AfterSale> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(AfterSale::getOrderItemId, orderItemId);
        itemWrapper.in(AfterSale::getAfterSaleStatus, "PENDING", "APPROVED", "PLATFORM_REVIEWING");
        List<AfterSale> itemAfterSales = afterSaleMapper.selectList(itemWrapper);

        if (itemAfterSales.isEmpty()) {
            OrderItem orderItem = orderItemMapper.selectById(orderItemId);
            if (orderItem != null) {
                // 检查是否有已完成的售后
                LambdaQueryWrapper<AfterSale> completedWrapper = new LambdaQueryWrapper<>();
                completedWrapper.eq(AfterSale::getOrderItemId, orderItemId);
                completedWrapper.eq(AfterSale::getAfterSaleStatus, "COMPLETED");
                List<AfterSale> completedAfterSales = afterSaleMapper.selectList(completedWrapper);

                if (!completedAfterSales.isEmpty()) {
                    orderItem.setItemAfterSalesStatus("COMPLETED");
                } else {
                    // 检查是否有被拒绝或平台已裁决的售后
                    LambdaQueryWrapper<AfterSale> rejectedWrapper = new LambdaQueryWrapper<>();
                    rejectedWrapper.eq(AfterSale::getOrderItemId, orderItemId);
                    rejectedWrapper.in(AfterSale::getAfterSaleStatus, "REJECTED", "PLATFORM_RESOLVED");
                    List<AfterSale> rejectedAfterSales = afterSaleMapper.selectList(rejectedWrapper);

                    if (!rejectedAfterSales.isEmpty()) {
                        orderItem.setItemAfterSalesStatus("REJECTED");
                    } else {
                        orderItem.setItemAfterSalesStatus("NONE");
                    }
                }
                orderItem.setUpdateTime(LocalDateTime.now());
                orderItemMapper.updateById(orderItem);
            }
        }

        // 检查该订单下是否还有进行中的售后
        LambdaQueryWrapper<AfterSale> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.eq(AfterSale::getOrderId, orderId);
        orderWrapper.in(AfterSale::getAfterSaleStatus, "PENDING", "APPROVED", "PLATFORM_REVIEWING");
        List<AfterSale> orderAfterSales = afterSaleMapper.selectList(orderWrapper);

        if (orderAfterSales.isEmpty()) {
            Order order = orderMapper.selectById(orderId);
            if (order != null) {
                // 检查是否有已完成的售后
                LambdaQueryWrapper<AfterSale> completedWrapper = new LambdaQueryWrapper<>();
                completedWrapper.eq(AfterSale::getOrderId, orderId);
                completedWrapper.eq(AfterSale::getAfterSaleStatus, "COMPLETED");
                List<AfterSale> completedAfterSales = afterSaleMapper.selectList(completedWrapper);

                if (!completedAfterSales.isEmpty()) {
                    order.setAfterSalesStatus("COMPLETED");
                } else {
                    // 检查是否有被拒绝或平台已裁决的售后
                    LambdaQueryWrapper<AfterSale> rejectedWrapper = new LambdaQueryWrapper<>();
                    rejectedWrapper.eq(AfterSale::getOrderId, orderId);
                    rejectedWrapper.in(AfterSale::getAfterSaleStatus, "REJECTED", "PLATFORM_RESOLVED");
                    List<AfterSale> rejectedAfterSales = afterSaleMapper.selectList(rejectedWrapper);

                    if (!rejectedAfterSales.isEmpty()) {
                        order.setAfterSalesStatus("REJECTED");
                    } else {
                        order.setAfterSalesStatus("NONE");
                    }
                }
                order.setUpdateTime(LocalDateTime.now());
                orderMapper.updateById(order);
            }
        }
    }

    public List<AfterSaleInfoDTO> getAfterSalesByUserId(String userId) {
        LambdaQueryWrapper<AfterSale> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AfterSale::getUserId, userId);
        wrapper.orderByDesc(AfterSale::getCreateTime);

        List<AfterSale> afterSales = afterSaleMapper.selectList(wrapper);
        return convertToDTOList(afterSales);
    }

    public List<AfterSaleInfoDTO> getAfterSalesByOrderId(String orderId) {
        LambdaQueryWrapper<AfterSale> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AfterSale::getOrderId, orderId);
        wrapper.orderByDesc(AfterSale::getCreateTime);

        List<AfterSale> afterSales = afterSaleMapper.selectList(wrapper);
        return convertToDTOList(afterSales);
    }

    public List<AfterSaleInfoDTO> getAfterSalesBySellerId(String sellerId) {
        LambdaQueryWrapper<AfterSale> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AfterSale::getSellerId, sellerId);
        wrapper.orderByDesc(AfterSale::getCreateTime);

        List<AfterSale> afterSales = afterSaleMapper.selectList(wrapper);
        return convertToDTOList(afterSales);
    }

    /**
     * 商家端售后列表查询（支持状态过滤）
     */
    public List<AfterSaleInfoDTO> getAfterSalesBySellerId(String sellerId, String status) {
        LambdaQueryWrapper<AfterSale> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AfterSale::getSellerId, sellerId);
        if (status != null && !status.isEmpty() && !"ALL".equals(status)) {
            wrapper.eq(AfterSale::getAfterSaleStatus, status);
        }
        wrapper.orderByDesc(AfterSale::getCreateTime);

        List<AfterSale> afterSales = afterSaleMapper.selectList(wrapper);
        return convertToDTOList(afterSales);
    }

    public AfterSaleInfoDTO getAfterSaleDetail(String afterSaleId) {
        AfterSale afterSale = afterSaleMapper.selectById(afterSaleId);
        if (afterSale == null) {
            return null;
        }
        return convertToDTO(afterSale);
    }

    // ==================== 平台管理端接口 ====================

    /**
     * 获取全部售后列表（管理端，支持过滤）
     */
    public List<AfterSaleInfoDTO> getAllAfterSales(String status, String sellerId, String userId, LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<AfterSale> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty() && !"ALL".equals(status)) {
            wrapper.eq(AfterSale::getAfterSaleStatus, status);
        }
        if (sellerId != null && !sellerId.isEmpty()) {
            wrapper.eq(AfterSale::getSellerId, sellerId);
        }
        if (userId != null && !userId.isEmpty()) {
            wrapper.eq(AfterSale::getUserId, userId);
        }
        if (startTime != null) {
            wrapper.ge(AfterSale::getCreateTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(AfterSale::getCreateTime, endTime);
        }
        wrapper.orderByDesc(AfterSale::getCreateTime);
        List<AfterSale> afterSales = afterSaleMapper.selectList(wrapper);
        return convertToDTOList(afterSales);
    }

    /**
     * 获取所有待平台介入的售后单
     */
    public List<AfterSaleInfoDTO> getPlatformInterventionList() {
        LambdaQueryWrapper<AfterSale> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AfterSale::getAfterSaleStatus, "PLATFORM_REVIEWING");
        wrapper.orderByAsc(AfterSale::getPlatformInterventionTime);
        List<AfterSale> afterSales = afterSaleMapper.selectList(wrapper);
        return convertToDTOList(afterSales);
    }

    /**
     * 获取所有订单（管理端，支持过滤）
     */
    public List<Order> getAllOrders(String sellerId, String userId, String status, LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        if (sellerId != null && !sellerId.isEmpty()) {
            // 通过 order_items 表的 item_seller_id 过滤
            LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
            itemWrapper.eq(OrderItem::getItemSellerId, sellerId);
            itemWrapper.select(OrderItem::getOrderId);
            List<OrderItem> items = orderItemMapper.selectList(itemWrapper);
            List<String> orderIds = new ArrayList<>();
            for (OrderItem item : items) {
                if (!orderIds.contains(item.getOrderId())) {
                    orderIds.add(item.getOrderId());
                }
            }
            if (orderIds.isEmpty()) {
                return new ArrayList<>();
            }
            wrapper.in(Order::getOrderId, orderIds);
        }
        if (userId != null && !userId.isEmpty()) {
            wrapper.eq(Order::getUserId, userId);
        }
        if (status != null && !status.isEmpty() && !"ALL".equals(status)) {
            wrapper.eq(Order::getOrderStatus, status);
        }
        if (startTime != null) {
            wrapper.ge(Order::getCreateTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(Order::getCreateTime, endTime);
        }
        wrapper.orderByDesc(Order::getCreateTime);
        return orderMapper.selectList(wrapper);
    }

    private List<AfterSaleInfoDTO> convertToDTOList(List<AfterSale> afterSales) {
        List<AfterSaleInfoDTO> dtoList = new ArrayList<>();
        for (AfterSale afterSale : afterSales) {
            dtoList.add(convertToDTO(afterSale));
        }
        return dtoList;
    }

    private AfterSaleInfoDTO convertToDTO(AfterSale afterSale) {
        AfterSaleInfoDTO dto = new AfterSaleInfoDTO();
        BeanUtils.copyProperties(afterSale, dto);

        OrderItem orderItem = orderItemMapper.selectById(afterSale.getOrderItemId());
        if (orderItem != null) {
            dto.setProductName(orderItem.getProductName());
            dto.setProductImage(orderItem.getProductImage());
            dto.setProductSpec(orderItem.getProductSpec());
        }

        // 填充订单号
        Order order = orderMapper.selectById(afterSale.getOrderId());
        if (order != null) {
            dto.setOrderNo(order.getOrderNo());
        }

        // 填充店铺信息
        if (afterSale.getSellerId() != null) {
            Shop shop = shopMapper.selectById(afterSale.getSellerId());
            if (shop != null) {
                dto.setShopName(shop.getShopName());
                dto.setShopId(shop.getShopId());
            }
        }

        // 填充用户信息
        if (afterSale.getUserId() != null) {
            User user = userMapper.selectById(afterSale.getUserId());
            if (user != null) {
                dto.setUsername(user.getUsername());
                dto.setUserAvatar(user.getAvatarUrl());
            }
        }

        return dto;
    }
}
