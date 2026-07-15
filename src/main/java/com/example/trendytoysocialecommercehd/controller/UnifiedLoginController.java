package com.example.trendytoysocialecommercehd.controller;

import com.example.trendytoysocialecommercehd.annotation.AuditLog;
import com.example.trendytoysocialecommercehd.common.Result;
import com.example.trendytoysocialecommercehd.dto.AdminLoginDTO;
import com.example.trendytoysocialecommercehd.dto.MerchantLoginDTO;
import com.example.trendytoysocialecommercehd.dto.MerchantRegisterDTO;
import com.example.trendytoysocialecommercehd.entity.PlatformAdmin;
import com.example.trendytoysocialecommercehd.entity.ShopAdmin;
import com.example.trendytoysocialecommercehd.service.PlatformAdminService;
import com.example.trendytoysocialecommercehd.service.ShopAdminService;
import com.example.trendytoysocialecommercehd.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 统一登录控制器 - 根据账号自动判断用户身份（商家/平台管理员）
 */
@RestController
@RequestMapping("/api")
public class UnifiedLoginController {

    @Autowired
    private ShopAdminService shopAdminService;

    @Autowired
    private PlatformAdminService platformAdminService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 统一登录接口 - 后端根据账号判断身份
     */
    @AuditLog(module = "AUTH", action = "LOGIN", description = "统一登录")
    @PostMapping("/login")
    public Result<?> unifiedLogin(@RequestBody AdminLoginDTO loginDTO) {
        String usernameOrPhone = loginDTO.getUsernameOrPhone();
        String password = loginDTO.getPassword();

        // 1. 先尝试商家登录
        try {
            MerchantLoginDTO merchantLoginDTO = new MerchantLoginDTO();
            merchantLoginDTO.setUsernameOrPhone(usernameOrPhone);
            merchantLoginDTO.setPassword(password);
            
            ShopAdmin shopAdmin = shopAdminService.login(merchantLoginDTO);
            String token = jwtUtil.generateTokenWithUserType(shopAdmin.getAdminId(), "merchant");

            Map<String, Object> result = new HashMap<>();
            result.put("token", token);
            result.put("userType", "merchant");
            result.put("user", shopAdmin);

            return Result.success(result);
        } catch (RuntimeException e) {
            // 商家登录失败，继续尝试平台管理员
        }

        // 2. 尝试平台管理员登录
        try {
            PlatformAdmin platformAdmin = platformAdminService.login(loginDTO);
            String token = jwtUtil.generateTokenWithUserType(platformAdmin.getAdminId(), "admin");

            Map<String, Object> result = new HashMap<>();
            result.put("token", token);
            result.put("userType", "admin");
            result.put("user", platformAdmin);

            return Result.success(result);
        } catch (RuntimeException e) {
            return Result.error("用户名或密码错误");
        }
    }

    /**
     * 统一注册接口 - 默认注册为商家
     */
    @PostMapping("/register")
    public Result<?> unifiedRegister(@RequestBody MerchantRegisterDTO registerDTO) {
        try {
            ShopAdmin shopAdmin = shopAdminService.register(registerDTO);
            String token = jwtUtil.generateTokenWithUserType(shopAdmin.getAdminId(), "merchant");

            Map<String, Object> result = new HashMap<>();
            result.put("token", token);
            result.put("userType", "merchant");
            result.put("user", shopAdmin);

            return Result.success(result);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }
}