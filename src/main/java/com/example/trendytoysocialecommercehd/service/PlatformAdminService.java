package com.example.trendytoysocialecommercehd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.trendytoysocialecommercehd.dto.AdminLoginDTO;
import com.example.trendytoysocialecommercehd.dto.AdminRegisterDTO;
import com.example.trendytoysocialecommercehd.entity.PlatformAdmin;
import com.example.trendytoysocialecommercehd.mapper.PlatformAdminMapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class PlatformAdminService extends ServiceImpl<PlatformAdminMapper, PlatformAdmin> {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public PlatformAdmin login(AdminLoginDTO loginDTO) {
        LambdaQueryWrapper<PlatformAdmin> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PlatformAdmin::getAdminId, loginDTO.getUsernameOrPhone());

        PlatformAdmin platformAdmin = this.getOne(wrapper);
        if (platformAdmin == null) {
            throw new RuntimeException("管理员不存在");
        }

        if (!passwordEncoder.matches(loginDTO.getPassword(), platformAdmin.getPasswordHash())) {
            throw new RuntimeException("密码错误");
        }

        if (!"active".equals(platformAdmin.getAccountStatus())) {
            throw new RuntimeException("账户已被禁用");
        }

        platformAdmin.setLastLoginTime(new Date());
        this.updateById(platformAdmin);

        return platformAdmin;
    }

    public PlatformAdmin register(AdminRegisterDTO registerDTO) {
        if (!registerDTO.getPassword().equals(registerDTO.getConfirmPassword())) {
            throw new RuntimeException("两次密码输入不一致");
        }

        LambdaQueryWrapper<PlatformAdmin> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PlatformAdmin::getAdminId, registerDTO.getUsername());

        if (this.count(wrapper) > 0) {
            throw new RuntimeException("用户名已存在");
        }

        PlatformAdmin platformAdmin = new PlatformAdmin();
        platformAdmin.setAdminId(registerDTO.getUsername());
        platformAdmin.setPasswordHash(passwordEncoder.encode(registerDTO.getPassword()));
        platformAdmin.setAccountStatus("active");
        platformAdmin.setActivatedAt(new Date());

        this.save(platformAdmin);
        return platformAdmin;
    }

    public PlatformAdmin getPlatformAdminById(String adminId) {
        return this.getById(adminId);
    }

    public Page<PlatformAdmin> getPlatformAdminList(int page, int size, String adminLevel, String accountStatus) {
        Page<PlatformAdmin> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<PlatformAdmin> wrapper = new LambdaQueryWrapper<>();

        if (adminLevel != null && !adminLevel.isEmpty()) {
            wrapper.eq(PlatformAdmin::getAdminLevel, adminLevel);
        }
        if (accountStatus != null && !accountStatus.isEmpty()) {
            wrapper.eq(PlatformAdmin::getAccountStatus, accountStatus);
        }
        wrapper.orderByDesc(PlatformAdmin::getLastLoginTime);

        return this.page(pageObj, wrapper);
    }

    public void createPlatformAdmin(PlatformAdmin platformAdmin) {
        platformAdmin.setPasswordHash(passwordEncoder.encode(platformAdmin.getPasswordHash()));
        platformAdmin.setAccountStatus(platformAdmin.getAccountStatus() != null ? platformAdmin.getAccountStatus() : "active");
        platformAdmin.setActivatedAt(new Date());
        this.save(platformAdmin);
    }

    public void updatePlatformAdmin(String adminId, PlatformAdmin platformAdmin) {
        PlatformAdmin existing = this.getById(adminId);
        if (existing == null) {
            throw new RuntimeException("管理员不存在");
        }

        if (platformAdmin.getPasswordHash() != null && !platformAdmin.getPasswordHash().isEmpty()) {
            platformAdmin.setPasswordHash(passwordEncoder.encode(platformAdmin.getPasswordHash()));
        } else {
            platformAdmin.setPasswordHash(existing.getPasswordHash());
        }

        platformAdmin.setAdminId(adminId);
        this.updateById(platformAdmin);
    }

    public void deletePlatformAdmin(String adminId) {
        this.removeById(adminId);
    }
}