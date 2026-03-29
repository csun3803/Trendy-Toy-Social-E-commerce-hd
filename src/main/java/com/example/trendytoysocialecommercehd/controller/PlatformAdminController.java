package com.example.trendytoysocialecommercehd.controller;

import com.example.trendytoysocialecommercehd.common.Result;
import com.example.trendytoysocialecommercehd.dto.AdminLoginDTO;
import com.example.trendytoysocialecommercehd.dto.AdminRegisterDTO;
import com.example.trendytoysocialecommercehd.entity.PlatformAdmin;
import com.example.trendytoysocialecommercehd.service.PlatformAdminService;
import com.example.trendytoysocialecommercehd.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class PlatformAdminController {

    @Autowired
    private PlatformAdminService platformAdminService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/login")
    public Result<?> login(@RequestBody AdminLoginDTO loginDTO) {
        try {
            PlatformAdmin platformAdmin = platformAdminService.login(loginDTO);
            String token = jwtUtil.generateToken(platformAdmin.getAdminId());

            Map<String, Object> result = new HashMap<>();
            result.put("user", platformAdmin);
            result.put("token", token);

            return Result.success(result);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/register")
    public Result<?> register(@RequestBody AdminRegisterDTO registerDTO) {
        try {
            PlatformAdmin platformAdmin = platformAdminService.register(registerDTO);
            return Result.success(platformAdmin);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/info/current")
    public Result<?> getCurrentAdminInfo(@RequestHeader("Authorization") String token) {
        try {
            String cleanToken = token.replace("Bearer ", "");
            if (!jwtUtil.validateToken(cleanToken)) {
                return Result.error("无效的token");
            }
            String adminId = jwtUtil.getUserIdFromToken(cleanToken);
            PlatformAdmin platformAdmin = platformAdminService.getPlatformAdminById(adminId);
            return Result.success(platformAdmin);
        } catch (Exception e) {
            return Result.error("获取管理员信息失败");
        }
    }
}