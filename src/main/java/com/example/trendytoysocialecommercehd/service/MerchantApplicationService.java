package com.example.trendytoysocialecommercehd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.trendytoysocialecommercehd.dto.MerchantApplyDTO;
import com.example.trendytoysocialecommercehd.entity.MerchantApplication;
import com.example.trendytoysocialecommercehd.entity.Shop;
import com.example.trendytoysocialecommercehd.entity.ShopAdmin;
import com.example.trendytoysocialecommercehd.mapper.MerchantApplicationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class MerchantApplicationService extends ServiceImpl<MerchantApplicationMapper, MerchantApplication> {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Autowired
    private ShopService shopService;

    @Autowired
    private ShopAdminService shopAdminService;

    /**
     * 提交入驻申请（注册+填写信息一步完成）
     */
    @Transactional
    public MerchantApplication submitApplication(MerchantApplyDTO dto) {
        // 检查手机号是否已注册
        LambdaQueryWrapper<ShopAdmin> adminWrapper = new LambdaQueryWrapper<>();
        adminWrapper.eq(ShopAdmin::getAdminId, dto.getMobile());
        if (shopAdminService.count(adminWrapper) > 0) {
            throw new RuntimeException("该手机号已注册");
        }

        // 检查手机号是否已有待审核的申请
        LambdaQueryWrapper<MerchantApplication> appWrapper = new LambdaQueryWrapper<>();
        appWrapper.eq(MerchantApplication::getMobile, dto.getMobile())
                  .in(MerchantApplication::getStatus, 0, 1);
        if (this.count(appWrapper) > 0) {
            throw new RuntimeException("该手机号已有待审核或已通过的申请");
        }

        // 检查店铺名是否重复
        LambdaQueryWrapper<MerchantApplication> nameWrapper = new LambdaQueryWrapper<>();
        nameWrapper.eq(MerchantApplication::getShopName, dto.getShopName())
                   .in(MerchantApplication::getStatus, 0, 1);
        if (this.count(nameWrapper) > 0) {
            throw new RuntimeException("该店铺名已被使用");
        }

        MerchantApplication app = new MerchantApplication();
        app.setApplySn(generateApplySn());
        app.setMobile(dto.getMobile());
        app.setPassword(passwordEncoder.encode(dto.getPassword()));
        app.setShopName(dto.getShopName());
        app.setContactName(dto.getContactName());
        app.setSubjectType(dto.getSubjectType());
        app.setLicenseNo(dto.getLicenseNo());
        app.setLicenseImage(dto.getLicenseImage());
        app.setIdCardNo(dto.getIdCardNo());
        app.setIdCardFront(dto.getIdCardFront());
        app.setIdCardBack(dto.getIdCardBack());
        app.setBankAccountName(dto.getBankAccountName());
        app.setBankName(dto.getBankName());
        app.setBankCardNo(dto.getBankCardNo());
        app.setStatus(0); // 待审核
        app.setApplyTime(LocalDateTime.now());

        this.save(app);

        // 同时创建 shop_admin 记录（状态为待审核，无 shopId）
        ShopAdmin shopAdmin = new ShopAdmin();
        shopAdmin.setAdminId(dto.getMobile());
        shopAdmin.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        shopAdmin.setIsActive(1);
        shopAdmin.setAuditStatus("待审核");
        shopAdmin.setLoginCount(0);
        shopAdminService.save(shopAdmin);

        return app;
    }

    /**
     * 商家注册（仅手机号+密码，后续再填申请表）
     */
    @Transactional
    public ShopAdmin registerByPhone(String mobile, String password) {
        LambdaQueryWrapper<ShopAdmin> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShopAdmin::getAdminId, mobile);
        if (shopAdminService.count(wrapper) > 0) {
            throw new RuntimeException("该手机号已注册");
        }

        ShopAdmin shopAdmin = new ShopAdmin();
        shopAdmin.setAdminId(mobile);
        shopAdmin.setPasswordHash(passwordEncoder.encode(password));
        shopAdmin.setIsActive(1);
        shopAdmin.setAuditStatus("待审核");
        shopAdmin.setLoginCount(0);
        shopAdminService.save(shopAdmin);

        return shopAdmin;
    }

    /**
     * 商家提交入驻申请（已注册后填写信息）
     */
    @Transactional
    public MerchantApplication submitApplicationAfterRegister(MerchantApplyDTO dto, String mobile) {
        // 检查是否已有待审核或已通过的申请
        LambdaQueryWrapper<MerchantApplication> appWrapper = new LambdaQueryWrapper<>();
        appWrapper.eq(MerchantApplication::getMobile, mobile)
                  .in(MerchantApplication::getStatus, 0, 1);
        if (this.count(appWrapper) > 0) {
            throw new RuntimeException("您已有待审核或已通过的申请");
        }

        // 检查店铺名是否重复
        LambdaQueryWrapper<MerchantApplication> nameWrapper = new LambdaQueryWrapper<>();
        nameWrapper.eq(MerchantApplication::getShopName, dto.getShopName())
                   .in(MerchantApplication::getStatus, 0, 1);
        if (this.count(nameWrapper) > 0) {
            throw new RuntimeException("该店铺名已被使用");
        }

        MerchantApplication app = new MerchantApplication();
        app.setApplySn(generateApplySn());
        app.setMobile(mobile);
        // 获取已注册账号的密码
        ShopAdmin existingAdmin = shopAdminService.getById(mobile);
        app.setPassword(existingAdmin.getPasswordHash());
        app.setShopName(dto.getShopName());
        app.setContactName(dto.getContactName());
        app.setSubjectType(dto.getSubjectType());
        app.setLicenseNo(dto.getLicenseNo());
        app.setLicenseImage(dto.getLicenseImage());
        app.setIdCardNo(dto.getIdCardNo());
        app.setIdCardFront(dto.getIdCardFront());
        app.setIdCardBack(dto.getIdCardBack());
        app.setBankAccountName(dto.getBankAccountName());
        app.setBankName(dto.getBankName());
        app.setBankCardNo(dto.getBankCardNo());
        app.setStatus(0);
        app.setApplyTime(LocalDateTime.now());

        this.save(app);

        // 更新 shop_admin 的审核状态
        existingAdmin.setAuditStatus("待审核");
        shopAdminService.updateById(existingAdmin);

        return app;
    }

    /**
     * 审核通过：创建 shop 记录 + 关联 shop_admin
     */
    @Transactional
    public void approveApplication(Long applicationId, String auditorId) {
        MerchantApplication app = this.getById(applicationId);
        if (app == null) {
            throw new RuntimeException("申请不存在");
        }
        if (app.getStatus() != 0) {
            throw new RuntimeException("该申请已处理");
        }

        // 更新申请状态
        app.setStatus(1);
        app.setAuditTime(LocalDateTime.now());
        this.updateById(app);

        // 创建 shop 记录
        Shop shop = new Shop();
        shop.setShopId("shop_" + System.currentTimeMillis());
        shop.setShopName(app.getShopName());
        shop.setShopType("普通店铺");
        shop.setBusinessEntityType(app.getSubjectType() == 0 ? "个人" : app.getSubjectType() == 1 ? "个体户" : "企业");
        shop.setLegalPersonName(app.getContactName());
        shop.setLegalPersonIdCard(app.getIdCardNo());
        shop.setUnifiedSocialCreditCode(app.getLicenseNo());
        shop.setBankName(app.getBankName());
        shop.setBankAccount(app.getBankCardNo());
        shop.setAccountHolder(app.getBankAccountName());
        shop.setCustomerServicePhone(app.getMobile());
        shop.setShopStatus("正常营业");
        shop.setBusinessStatus("营业中");
        shop.setAuditStatus("已通过");
        shop.setAuditNotes("入驻申请审核通过");
        shop.setAuditedAt(LocalDateTime.now());
        shop.setAuditorId(auditorId);
        shop.setAuditRound(1);
        shop.setShopRating(java.math.BigDecimal.valueOf(5.0));
        shop.setDepositStatus("未缴纳");
        shopService.createShop(shop);

        // 关联 shop_admin
        ShopAdmin shopAdmin = shopAdminService.getById(app.getMobile());
        if (shopAdmin != null) {
            shopAdmin.setShopId(shop.getShopId());
            shopAdmin.setAuditStatus("已通过");
            shopAdmin.setIsActive(1);
            shopAdminService.updateById(shopAdmin);
        }
    }

    /**
     * 审核驳回
     */
    @Transactional
    public void rejectApplication(Long applicationId, String auditRemark) {
        MerchantApplication app = this.getById(applicationId);
        if (app == null) {
            throw new RuntimeException("申请不存在");
        }
        if (app.getStatus() != 0) {
            throw new RuntimeException("该申请已处理");
        }

        app.setStatus(2);
        app.setAuditRemark(auditRemark);
        app.setAuditTime(LocalDateTime.now());
        this.updateById(app);

        // 更新 shop_admin 审核状态
        ShopAdmin shopAdmin = shopAdminService.getById(app.getMobile());
        if (shopAdmin != null) {
            shopAdmin.setAuditStatus("已拒绝");
            shopAdmin.setAuditNotes(auditRemark);
            shopAdminService.updateById(shopAdmin);
        }
    }

    /**
     * 获取申请列表（分页）
     */
    public Page<MerchantApplication> getApplicationList(int page, int size, Integer status) {
        Page<MerchantApplication> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<MerchantApplication> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(MerchantApplication::getStatus, status);
        }
        wrapper.orderByDesc(MerchantApplication::getApplyTime);
        return this.page(pageObj, wrapper);
    }

    /**
     * 根据手机号查询申请
     */
    public MerchantApplication getApplicationByMobile(String mobile) {
        LambdaQueryWrapper<MerchantApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MerchantApplication::getMobile, mobile)
               .orderByDesc(MerchantApplication::getApplyTime)
               .last("LIMIT 1");
        return this.getOne(wrapper);
    }

    /**
     * 生成申请单号
     */
    private String generateApplySn() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int random = ThreadLocalRandom.current().nextInt(1000, 9999);
        return "MA" + timestamp + random;
    }
}
