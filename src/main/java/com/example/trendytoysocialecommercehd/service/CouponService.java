package com.example.trendytoysocialecommercehd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.trendytoysocialecommercehd.dto.AvailableCouponDTO;
import com.example.trendytoysocialecommercehd.dto.CouponTemplateRequest;
import com.example.trendytoysocialecommercehd.dto.IssueCouponRequest;
import com.example.trendytoysocialecommercehd.dto.UserCouponDTO;
import com.example.trendytoysocialecommercehd.entity.CouponTemplate;
import com.example.trendytoysocialecommercehd.entity.User;
import com.example.trendytoysocialecommercehd.entity.UserCoupon;
import com.example.trendytoysocialecommercehd.mapper.CouponTemplateMapper;
import com.example.trendytoysocialecommercehd.mapper.UserCouponMapper;
import com.example.trendytoysocialecommercehd.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CouponService {

    @Autowired
    private CouponTemplateMapper couponTemplateMapper;

    @Autowired
    private UserCouponMapper userCouponMapper;

    @Autowired
    private UserMapper userMapper;

    // ==================== 模板管理 ====================

    @Transactional(rollbackFor = Exception.class)
    public CouponTemplate createTemplate(CouponTemplateRequest request) {
        validateTemplateRequest(request);
        CouponTemplate template = new CouponTemplate();
        template.setTemplateId(UUID.randomUUID().toString());
        template.setName(request.getName());
        template.setType(request.getType() != null ? request.getType() : "FULL_REDUCTION");
        template.setDiscountValue(request.getDiscountValue());
        template.setMinSpend(request.getMinSpend() != null ? request.getMinSpend() : BigDecimal.ZERO);
        template.setValidFrom(request.getValidFrom());
        template.setValidTo(request.getValidTo());
        template.setValidDays(request.getValidDays() != null ? request.getValidDays() : 30);
        template.setTotalQuantity(request.getTotalQuantity() != null ? request.getTotalQuantity() : 0);
        template.setUserLimit(request.getUserLimit() != null ? request.getUserLimit() : 1);
        template.setStatus(request.getStatus() != null ? request.getStatus() : "active");
        template.setCreateTime(LocalDateTime.now());
        template.setUpdateTime(LocalDateTime.now());
        couponTemplateMapper.insert(template);
        return template;
    }

    @Transactional(rollbackFor = Exception.class)
    public CouponTemplate updateTemplate(CouponTemplateRequest request) {
        if (request.getTemplateId() == null || request.getTemplateId().isEmpty()) {
            throw new RuntimeException("模板ID不能为空");
        }
        CouponTemplate template = couponTemplateMapper.selectById(request.getTemplateId());
        if (template == null) {
            throw new RuntimeException("模板不存在");
        }
        validateTemplateRequest(request);
        template.setName(request.getName());
        template.setType(request.getType() != null ? request.getType() : template.getType());
        template.setDiscountValue(request.getDiscountValue());
        template.setMinSpend(request.getMinSpend() != null ? request.getMinSpend() : BigDecimal.ZERO);
        template.setValidFrom(request.getValidFrom());
        template.setValidTo(request.getValidTo());
        template.setValidDays(request.getValidDays() != null ? request.getValidDays() : template.getValidDays());
        template.setTotalQuantity(request.getTotalQuantity() != null ? request.getTotalQuantity() : template.getTotalQuantity());
        template.setUserLimit(request.getUserLimit() != null ? request.getUserLimit() : template.getUserLimit());
        template.setStatus(request.getStatus() != null ? request.getStatus() : template.getStatus());
        template.setUpdateTime(LocalDateTime.now());
        couponTemplateMapper.updateById(template);
        return template;
    }

    private void validateTemplateRequest(CouponTemplateRequest request) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new RuntimeException("模板名称不能为空");
        }
        if (request.getDiscountValue() == null || request.getDiscountValue().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("减扣金额必须大于0");
        }
        if (request.getValidFrom() == null || request.getValidTo() == null) {
            throw new RuntimeException("有效日期不能为空");
        }
        if (request.getValidTo().isBefore(request.getValidFrom())) {
            throw new RuntimeException("失效日期不能早于生效日期");
        }
        if (request.getMinSpend() != null && request.getMinSpend().compareTo(request.getDiscountValue()) < 0) {
            throw new RuntimeException("满减门槛不能小于减扣金额");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public CouponTemplate toggleTemplateStatus(String templateId, String status) {
        CouponTemplate template = couponTemplateMapper.selectById(templateId);
        if (template == null) {
            throw new RuntimeException("模板不存在");
        }
        if (!"active".equals(status) && !"inactive".equals(status)) {
            throw new RuntimeException("状态值非法，仅支持 active/inactive");
        }
        template.setStatus(status);
        template.setUpdateTime(LocalDateTime.now());
        couponTemplateMapper.updateById(template);
        return template;
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteTemplate(String templateId) {
        CouponTemplate template = couponTemplateMapper.selectById(templateId);
        if (template == null) {
            throw new RuntimeException("模板不存在");
        }
        // 检查是否还有未使用的券，若有则禁止删除
        LambdaQueryWrapper<UserCoupon> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserCoupon::getTemplateId, templateId);
        wrapper.eq(UserCoupon::getStatus, "unused");
        Long unusedCount = userCouponMapper.selectCount(wrapper);
        if (unusedCount > 0) {
            throw new RuntimeException("存在未使用的券，无法删除模板");
        }
        couponTemplateMapper.deleteById(templateId);
    }

    public CouponTemplate getTemplate(String templateId) {
        return couponTemplateMapper.selectById(templateId);
    }

    public List<CouponTemplate> listTemplates(String name, String status) {
        LambdaQueryWrapper<CouponTemplate> wrapper = new LambdaQueryWrapper<>();
        if (name != null && !name.trim().isEmpty()) {
            wrapper.like(CouponTemplate::getName, name);
        }
        if (status != null && !status.trim().isEmpty()) {
            wrapper.eq(CouponTemplate::getStatus, status);
        }
        wrapper.orderByDesc(CouponTemplate::getCreateTime);
        return couponTemplateMapper.selectList(wrapper);
    }

    // ==================== 发券 ====================

    @Transactional(rollbackFor = Exception.class)
    public List<UserCoupon> issueCoupons(IssueCouponRequest request) {
        if (request.getTemplateId() == null) {
            throw new RuntimeException("请选择模板");
        }
        if (request.getUserIds() == null || request.getUserIds().isEmpty()) {
            throw new RuntimeException("请选择用户");
        }
        CouponTemplate template = couponTemplateMapper.selectById(request.getTemplateId());
        if (template == null) {
            throw new RuntimeException("模板不存在");
        }
        if (!"active".equals(template.getStatus())) {
            throw new RuntimeException("模板已停用，无法发券");
        }
        LocalDate today = LocalDate.now();
        if (today.isBefore(template.getValidFrom()) || today.isAfter(template.getValidTo())) {
            throw new RuntimeException("模板不在有效发券期内");
        }

        // 计算已发放数量
        LambdaQueryWrapper<UserCoupon> countWrapper = new LambdaQueryWrapper<>();
        countWrapper.eq(UserCoupon::getTemplateId, request.getTemplateId());
        Long issuedCount = userCouponMapper.selectCount(countWrapper);

        List<UserCoupon> result = new ArrayList<>();
        for (String userId : request.getUserIds()) {
            // 校验用户是否存在
            User user = userMapper.selectById(userId);
            if (user == null) {
                throw new RuntimeException("用户不存在: " + userId);
            }
            // 校验每人限领
            if (template.getUserLimit() != null && template.getUserLimit() > 0) {
                LambdaQueryWrapper<UserCoupon> userCountWrapper = new LambdaQueryWrapper<>();
                userCountWrapper.eq(UserCoupon::getTemplateId, request.getTemplateId());
                userCountWrapper.eq(UserCoupon::getUserId, userId);
                Long userIssuedCount = userCouponMapper.selectCount(userCountWrapper);
                if (userIssuedCount >= template.getUserLimit()) {
                    throw new RuntimeException("用户 " + (user.getUsername() != null ? user.getUsername() : userId)
                            + " 已达领取上限");
                }
            }
            // 校验总发放量
            if (template.getTotalQuantity() != null && template.getTotalQuantity() > 0) {
                if (issuedCount >= template.getTotalQuantity()) {
                    throw new RuntimeException("模板发放总量已用完");
                }
            }

            UserCoupon coupon = new UserCoupon();
            coupon.setUserCouponId(UUID.randomUUID().toString());
            coupon.setUserId(userId);
            coupon.setTemplateId(template.getTemplateId());
            coupon.setCouponCode(generateCouponCode());
            coupon.setStatus("unused");
            coupon.setClaimedAt(LocalDateTime.now());
            coupon.setExpiresAt(today.plusDays(template.getValidDays() != null ? template.getValidDays() : 30));
            coupon.setCreateTime(LocalDateTime.now());
            coupon.setUpdateTime(LocalDateTime.now());
            userCouponMapper.insert(coupon);
            result.add(coupon);

            issuedCount++;

            // 同步更新 user.couponCount
            if (user.getCouponCount() == null) {
                user.setCouponCount(0);
            }
            user.setCouponCount(user.getCouponCount() + 1);
            userMapper.updateById(user);
        }
        return result;
    }

    private String generateCouponCode() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String timePart = LocalDateTime.now().format(fmt);
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        return "CP" + timePart + random;
    }

    // ==================== 用户券查询 ====================

    public List<UserCouponDTO> getUserCoupons(String userId, String status) {
        LambdaQueryWrapper<UserCoupon> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserCoupon::getUserId, userId);
        if (status != null && !status.trim().isEmpty()) {
            wrapper.eq(UserCoupon::getStatus, status);
        }
        wrapper.orderByDesc(UserCoupon::getClaimedAt);
        List<UserCoupon> coupons = userCouponMapper.selectList(wrapper);
        if (coupons.isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> templateIds = coupons.stream().map(UserCoupon::getTemplateId).collect(Collectors.toSet());
        Map<String, CouponTemplate> templateMap = new HashMap<>();
        for (String tid : templateIds) {
            CouponTemplate t = couponTemplateMapper.selectById(tid);
            if (t != null) {
                templateMap.put(tid, t);
            }
        }
        return coupons.stream().map(c -> toUserCouponDTO(c, templateMap.get(c.getTemplateId()), null)).collect(Collectors.toList());
    }

    public List<UserCouponDTO> listAllUserCoupons(String templateId, String userId, String status) {
        LambdaQueryWrapper<UserCoupon> wrapper = new LambdaQueryWrapper<>();
        if (templateId != null && !templateId.trim().isEmpty()) {
            wrapper.eq(UserCoupon::getTemplateId, templateId);
        }
        if (userId != null && !userId.trim().isEmpty()) {
            wrapper.eq(UserCoupon::getUserId, userId);
        }
        if (status != null && !status.trim().isEmpty()) {
            wrapper.eq(UserCoupon::getStatus, status);
        }
        wrapper.orderByDesc(UserCoupon::getClaimedAt);
        List<UserCoupon> coupons = userCouponMapper.selectList(wrapper);
        if (coupons.isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> templateIds = coupons.stream().map(UserCoupon::getTemplateId).collect(Collectors.toSet());
        Set<String> userIds = coupons.stream().map(UserCoupon::getUserId).collect(Collectors.toSet());
        Map<String, CouponTemplate> templateMap = new HashMap<>();
        for (String tid : templateIds) {
            CouponTemplate t = couponTemplateMapper.selectById(tid);
            if (t != null) {
                templateMap.put(tid, t);
            }
        }
        Map<String, User> userMap = new HashMap<>();
        for (String uid : userIds) {
            User u = userMapper.selectById(uid);
            if (u != null) {
                userMap.put(uid, u);
            }
        }
        return coupons.stream()
                .map(c -> toUserCouponDTO(c, templateMap.get(c.getTemplateId()), userMap.get(c.getUserId())))
                .collect(Collectors.toList());
    }

    private UserCouponDTO toUserCouponDTO(UserCoupon coupon, CouponTemplate template, User user) {
        UserCouponDTO dto = new UserCouponDTO();
        dto.setUserCouponId(coupon.getUserCouponId());
        dto.setUserId(coupon.getUserId());
        dto.setTemplateId(coupon.getTemplateId());
        dto.setCouponCode(coupon.getCouponCode());
        dto.setStatus(coupon.getStatus());
        dto.setClaimedAt(coupon.getClaimedAt());
        dto.setUsedAt(coupon.getUsedAt());
        dto.setExpiresAt(coupon.getExpiresAt());
        dto.setOrderId(coupon.getOrderId());
        if (template != null) {
            dto.setTemplateName(template.getName());
            dto.setType(template.getType());
            dto.setDiscountValue(template.getDiscountValue());
            dto.setMinSpend(template.getMinSpend());
        }
        if (user != null) {
            dto.setUsername(user.getUsername());
            dto.setPhoneNumber(user.getPhoneNumber());
        }
        return dto;
    }

    // ==================== 作废用户券 ====================

    @Transactional(rollbackFor = Exception.class)
    public UserCoupon revokeUserCoupon(String userCouponId) {
        UserCoupon coupon = userCouponMapper.selectById(userCouponId);
        if (coupon == null) {
            throw new RuntimeException("券不存在");
        }
        if (!"unused".equals(coupon.getStatus())) {
            throw new RuntimeException("仅未使用的券可作废");
        }
        coupon.setStatus("revoked");
        coupon.setUpdateTime(LocalDateTime.now());
        userCouponMapper.updateById(coupon);

        // 同步扣减 user.couponCount
        User user = userMapper.selectById(coupon.getUserId());
        if (user != null && user.getCouponCount() != null && user.getCouponCount() > 0) {
            user.setCouponCount(user.getCouponCount() - 1);
            userMapper.updateById(user);
        }
        return coupon;
    }

    // ==================== 下单可用券 ====================

    public List<AvailableCouponDTO> getAvailableCoupons(String userId, BigDecimal orderAmount) {
        List<UserCouponDTO> userCoupons = getUserCoupons(userId, "unused");
        List<AvailableCouponDTO> result = new ArrayList<>();
        for (UserCouponDTO uc : userCoupons) {
            AvailableCouponDTO dto = new AvailableCouponDTO();
            dto.setUserCouponId(uc.getUserCouponId());
            dto.setTemplateId(uc.getTemplateId());
            dto.setTemplateName(uc.getTemplateName());
            dto.setDiscountValue(uc.getDiscountValue());
            dto.setMinSpend(uc.getMinSpend());
            dto.setExpiresAt(uc.getExpiresAt());
            dto.setCouponCode(uc.getCouponCode());

            BigDecimal minSpend = uc.getMinSpend() != null ? uc.getMinSpend() : BigDecimal.ZERO;
            boolean usable = orderAmount != null
                    && orderAmount.compareTo(minSpend) >= 0
                    && uc.getDiscountValue() != null;
            dto.setUsable(usable);
            BigDecimal discount = usable ? uc.getDiscountValue() : BigDecimal.ZERO;
            dto.setDiscountAmount(discount);
            BigDecimal payable = orderAmount != null ? orderAmount.subtract(discount) : BigDecimal.ZERO;
            if (payable.compareTo(BigDecimal.ZERO) < 0) {
                payable = BigDecimal.ZERO;
            }
            dto.setPayableAmount(payable);
            result.add(dto);
        }
        // 可用的排前面
        result.sort((a, b) -> {
            if (Boolean.TRUE.equals(a.getUsable()) && !Boolean.TRUE.equals(b.getUsable())) return -1;
            if (!Boolean.TRUE.equals(a.getUsable()) && Boolean.TRUE.equals(b.getUsable())) return 1;
            return 0;
        });
        return result;
    }

    /** 校验券可用性并返回扣减金额 */
    public BigDecimal validateAndCalcDiscount(String userCouponId, BigDecimal orderAmount) {
        if (userCouponId == null || userCouponId.isEmpty()) {
            return BigDecimal.ZERO;
        }
        UserCoupon coupon = userCouponMapper.selectById(userCouponId);
        if (coupon == null) {
            throw new RuntimeException("券不存在");
        }
        if (!"unused".equals(coupon.getStatus())) {
            throw new RuntimeException("券状态不可用");
        }
        if (coupon.getExpiresAt() != null && coupon.getExpiresAt().isBefore(LocalDate.now())) {
            throw new RuntimeException("券已过期");
        }
        CouponTemplate template = couponTemplateMapper.selectById(coupon.getTemplateId());
        if (template == null) {
            throw new RuntimeException("券模板不存在");
        }
        BigDecimal minSpend = template.getMinSpend() != null ? template.getMinSpend() : BigDecimal.ZERO;
        if (orderAmount == null || orderAmount.compareTo(minSpend) < 0) {
            throw new RuntimeException("订单金额不满足券使用门槛");
        }
        return template.getDiscountValue();
    }

    // ==================== 券状态变更（订单流程调用） ====================

    @Transactional(rollbackFor = Exception.class)
    public void useCoupon(String userCouponId, String orderId) {
        if (userCouponId == null || userCouponId.isEmpty()) {
            return;
        }
        UserCoupon coupon = userCouponMapper.selectById(userCouponId);
        if (coupon == null) {
            throw new RuntimeException("券不存在");
        }
        if (!"unused".equals(coupon.getStatus())) {
            throw new RuntimeException("券状态不可用，无法使用");
        }
        coupon.setStatus("used");
        coupon.setUsedAt(LocalDateTime.now());
        coupon.setOrderId(orderId);
        coupon.setUpdateTime(LocalDateTime.now());
        userCouponMapper.updateById(coupon);

        // 同步扣减 user.couponCount
        User user = userMapper.selectById(coupon.getUserId());
        if (user != null && user.getCouponCount() != null && user.getCouponCount() > 0) {
            user.setCouponCount(user.getCouponCount() - 1);
            userMapper.updateById(user);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void restoreCoupon(String userCouponId) {
        if (userCouponId == null || userCouponId.isEmpty()) {
            return;
        }
        UserCoupon coupon = userCouponMapper.selectById(userCouponId);
        if (coupon == null) {
            return;
        }
        restoreCouponEntity(coupon);
    }

    /** 通过订单ID恢复券（订单取消时调用） */
    @Transactional(rollbackFor = Exception.class)
    public void restoreCouponByOrderId(String orderId) {
        if (orderId == null || orderId.isEmpty()) {
            return;
        }
        LambdaQueryWrapper<UserCoupon> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserCoupon::getOrderId, orderId);
        wrapper.eq(UserCoupon::getStatus, "used");
        List<UserCoupon> coupons = userCouponMapper.selectList(wrapper);
        for (UserCoupon coupon : coupons) {
            restoreCouponEntity(coupon);
        }
    }

    private void restoreCouponEntity(UserCoupon coupon) {
        if (!"used".equals(coupon.getStatus())) {
            return;
        }
        // 若已过期则直接置为过期，否则恢复未使用
        if (coupon.getExpiresAt() != null && coupon.getExpiresAt().isBefore(LocalDate.now())) {
            coupon.setStatus("expired");
        } else {
            coupon.setStatus("unused");
        }
        coupon.setUsedAt(null);
        coupon.setOrderId(null);
        coupon.setUpdateTime(LocalDateTime.now());
        userCouponMapper.updateById(coupon);

        // 同步增加 user.couponCount（仅当恢复为未使用时）
        if ("unused".equals(coupon.getStatus())) {
            User user = userMapper.selectById(coupon.getUserId());
            if (user != null) {
                if (user.getCouponCount() == null) {
                    user.setCouponCount(0);
                }
                user.setCouponCount(user.getCouponCount() + 1);
                userMapper.updateById(user);
            }
        }
    }

    // ==================== 过期处理（定时任务） ====================

    /**
     * 每天凌晨 1:00 自动将过期未使用的券置为 expired
     */
    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void expireOverdueCoupons() {
        LambdaUpdateWrapper<UserCoupon> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(UserCoupon::getStatus, "unused");
        updateWrapper.lt(UserCoupon::getExpiresAt, LocalDate.now());
        updateWrapper.set(UserCoupon::getStatus, "expired");
        updateWrapper.set(UserCoupon::getUpdateTime, LocalDateTime.now());
        userCouponMapper.update(null, updateWrapper);
    }

    public int getUserCouponCount(String userId) {
        LambdaQueryWrapper<UserCoupon> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserCoupon::getUserId, userId);
        wrapper.eq(UserCoupon::getStatus, "unused");
        return Math.toIntExact(userCouponMapper.selectCount(wrapper));
    }
}
