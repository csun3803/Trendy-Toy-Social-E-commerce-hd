package com.example.trendytoysocialecommercehd.controller;

import com.example.trendytoysocialecommercehd.common.Result;
import com.example.trendytoysocialecommercehd.dto.MerchantLoginDTO;
import com.example.trendytoysocialecommercehd.dto.MerchantRegisterDTO;
import com.example.trendytoysocialecommercehd.entity.ShopAdmin;
import com.example.trendytoysocialecommercehd.service.ShopAdminService;
import com.example.trendytoysocialecommercehd.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/merchant")
public class MerchantController {

    @Autowired
    private ShopAdminService shopAdminService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/login")
    public Result<?> login(@RequestBody MerchantLoginDTO loginDTO) {
        try {
            ShopAdmin shopAdmin = shopAdminService.login(loginDTO);
            String token = jwtUtil.generateToken(shopAdmin.getAdminId());

            Map<String, Object> result = new HashMap<>();
            result.put("user", shopAdmin);
            result.put("token", token);

            return Result.success(result);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/register")
    public Result<?> register(@RequestBody MerchantRegisterDTO registerDTO) {
        try {
            ShopAdmin shopAdmin = shopAdminService.register(registerDTO);
            return Result.success(shopAdmin);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/info/current")
    public Result<?> getCurrentMerchantInfo(@RequestHeader("Authorization") String token) {
        try {
            String cleanToken = token.replace("Bearer ", "");

            if (!jwtUtil.validateToken(cleanToken)) {
                return Result.error("无效的token");
            }

            String adminId = jwtUtil.getUserIdFromToken(cleanToken);
            ShopAdmin shopAdmin = shopAdminService.getShopAdminById(adminId);
            return Result.success(shopAdmin);
        } catch (Exception e) {
            return Result.error("获取商户信息失败");
        }
    }
}