package com.example.trendytoysocialecommercehd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.trendytoysocialecommercehd.dto.MerchantLoginDTO;
import com.example.trendytoysocialecommercehd.dto.MerchantRegisterDTO;
import com.example.trendytoysocialecommercehd.entity.Shop;
import com.example.trendytoysocialecommercehd.entity.ShopAdmin;
import com.example.trendytoysocialecommercehd.mapper.ShopAdminMapper;
import com.example.trendytoysocialecommercehd.mapper.ShopMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class ShopAdminService extends ServiceImpl<ShopAdminMapper, ShopAdmin> {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public ShopAdmin login(MerchantLoginDTO loginDTO) {
        LambdaQueryWrapper<ShopAdmin> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShopAdmin::getAdminId, loginDTO.getUsernameOrPhone());

        ShopAdmin shopAdmin = this.getOne(wrapper);
        if (shopAdmin == null) {
            throw new RuntimeException("商户不存在");
        }

        if (!passwordEncoder.matches(loginDTO.getPassword(), shopAdmin.getPasswordHash())) {
            throw new RuntimeException("密码错误");
        }

        if (shopAdmin.getIsActive() != 1) {
            throw new RuntimeException("账户已被禁用");
        }

        shopAdmin.setLastLoginTime(new Date());
        shopAdmin.setLoginCount(shopAdmin.getLoginCount() + 1);
        this.updateById(shopAdmin);

        return shopAdmin;
    }

    /**
     * 通过手机号注册商家账号
     */
    public ShopAdmin registerByPhone(String mobile, String password) {
        LambdaQueryWrapper<ShopAdmin> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShopAdmin::getAdminId, mobile);

        if (this.count(wrapper) > 0) {
            throw new RuntimeException("该手机号已注册");
        }

        ShopAdmin shopAdmin = new ShopAdmin();
        shopAdmin.setAdminId(mobile);
        shopAdmin.setPasswordHash(passwordEncoder.encode(password));
        shopAdmin.setIsActive(1);
        shopAdmin.setAuditStatus("PENDING");
        shopAdmin.setLoginCount(0);

        this.save(shopAdmin);
        return shopAdmin;
    }

    public ShopAdmin register(MerchantRegisterDTO registerDTO) {
        if (!registerDTO.getPassword().equals(registerDTO.getConfirmPassword())) {
            throw new RuntimeException("两次密码输入不一致");
        }

        LambdaQueryWrapper<ShopAdmin> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShopAdmin::getAdminId, registerDTO.getUsername());

        if (this.count(wrapper) > 0) {
            throw new RuntimeException("用户名已存在");
        }

        ShopAdmin shopAdmin = new ShopAdmin();
        shopAdmin.setAdminId(registerDTO.getUsername());
        shopAdmin.setPasswordHash(passwordEncoder.encode(registerDTO.getPassword()));
        shopAdmin.setIsActive(1);
        shopAdmin.setAuditStatus("PENDING");
        shopAdmin.setLoginCount(0);

        this.save(shopAdmin);
        return shopAdmin;
    }

    public ShopAdmin getShopAdminById(String adminId) {
        return this.getById(adminId);
    }

    @Autowired
    private ShopMapper shopMapper;

    public Page<ShopAdmin> getShopAdminList(int page, int size, String auditStatus, String isActive) {
        Page<ShopAdmin> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<ShopAdmin> wrapper = new LambdaQueryWrapper<>();

        if (auditStatus != null && !auditStatus.isEmpty()) {
            wrapper.eq(ShopAdmin::getAuditStatus, auditStatus);
        }
        if (isActive != null && !isActive.isEmpty()) {
            wrapper.eq(ShopAdmin::getIsActive, Integer.parseInt(isActive));
        }
        wrapper.orderByDesc(ShopAdmin::getLastLoginTime);

        Page<ShopAdmin> result = this.page(pageObj, wrapper);

        // 填充 shopName（关联 shop 表）
        for (ShopAdmin admin : result.getRecords()) {
            if (admin.getShopId() != null && !admin.getShopId().isEmpty()) {
                Shop shop = shopMapper.selectById(admin.getShopId());
                if (shop != null) {
                    admin.setShopName(shop.getShopName());
                }
            }
        }

        return result;
    }

    public void createShopAdmin(ShopAdmin shopAdmin) {
        shopAdmin.setPasswordHash(passwordEncoder.encode(shopAdmin.getPasswordHash()));
        shopAdmin.setIsActive(shopAdmin.getIsActive() != null ? shopAdmin.getIsActive() : 1);
        shopAdmin.setLoginCount(0);
        shopAdmin.setAuditStatus("PENDING");
        this.save(shopAdmin);
    }

    public void updateShopAdmin(String adminId, ShopAdmin shopAdmin) {
        ShopAdmin existing = this.getById(adminId);
        if (existing == null) {
            throw new RuntimeException("管理员不存在");
        }

        if (shopAdmin.getPasswordHash() != null && !shopAdmin.getPasswordHash().isEmpty()) {
            shopAdmin.setPasswordHash(passwordEncoder.encode(shopAdmin.getPasswordHash()));
        } else {
            shopAdmin.setPasswordHash(existing.getPasswordHash());
        }

        shopAdmin.setAdminId(adminId);
        this.updateById(shopAdmin);
    }

    public void deleteShopAdmin(String adminId) {
        this.removeById(adminId);
    }

    public void updateShopId(String adminId, String shopId) {
        ShopAdmin shopAdmin = this.getById(adminId);
        if (shopAdmin == null) {
            throw new RuntimeException("管理员不存在");
        }
        shopAdmin.setShopId(shopId);
        this.updateById(shopAdmin);
    }

    /**
     * 重置商家管理员密码为 123456
     */
    public void resetPassword(String adminId) {
        ShopAdmin shopAdmin = this.getById(adminId);
        if (shopAdmin == null) {
            throw new RuntimeException("管理员不存在");
        }
        shopAdmin.setPasswordHash(passwordEncoder.encode("123456"));
        this.updateById(shopAdmin);
    }

    /**
     * 切换商家管理员启用/禁用状态
     */
    public void toggleStatus(String adminId, Integer isActive) {
        ShopAdmin shopAdmin = this.getById(adminId);
        if (shopAdmin == null) {
            throw new RuntimeException("管理员不存在");
        }
        shopAdmin.setIsActive(isActive);
        this.updateById(shopAdmin);
    }
}