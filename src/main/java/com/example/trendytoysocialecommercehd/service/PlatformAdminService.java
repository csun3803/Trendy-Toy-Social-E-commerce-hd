package com.example.trendytoysocialecommercehd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
}