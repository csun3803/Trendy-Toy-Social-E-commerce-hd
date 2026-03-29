package com.example.trendytoysocialecommercehd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.trendytoysocialecommercehd.dto.MerchantLoginDTO;
import com.example.trendytoysocialecommercehd.dto.MerchantRegisterDTO;
import com.example.trendytoysocialecommercehd.entity.ShopAdmin;
import com.example.trendytoysocialecommercehd.mapper.ShopAdminMapper;
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
        shopAdmin.setAuditStatus("已通过");
        shopAdmin.setLoginCount(0);

        this.save(shopAdmin);
        return shopAdmin;
    }

    public ShopAdmin getShopAdminById(String adminId) {
        return this.getById(adminId);
    }
}