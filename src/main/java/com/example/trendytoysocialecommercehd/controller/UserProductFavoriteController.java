package com.example.trendytoysocialecommercehd.controller;

import com.example.trendytoysocialecommercehd.common.Result;
import com.example.trendytoysocialecommercehd.entity.UserProductFavorite;
import com.example.trendytoysocialecommercehd.service.UserProductFavoriteService;
import com.example.trendytoysocialecommercehd.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/favorite")
public class UserProductFavoriteController {

    @Autowired
    private UserProductFavoriteService favoriteService;

    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping("/check/{productId}")
    public Result<Map<String, Object>> checkFavorite(
            @PathVariable String productId,
            HttpServletRequest request) {
        try {
            String userId = getUserIdFromRequest(request);
            boolean isFavorite = favoriteService.isFavorite(userId, productId);
            Map<String, Object> result = new HashMap<>();
            result.put("isFavorite", isFavorite);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("检查收藏状态失败: " + e.getMessage());
        }
    }

    @PostMapping("/toggle")
    public Result<Map<String, Object>> toggleFavorite(
            @RequestBody Map<String, String> requestBody,
            HttpServletRequest request) {
        try {
            String userId = getUserIdFromRequest(request);
            String productId = requestBody.get("productId");
            boolean success = favoriteService.toggleFavorite(userId, productId);
            boolean isFavorite = favoriteService.isFavorite(userId, productId);
            Map<String, Object> result = new HashMap<>();
            result.put("isFavorite", isFavorite);
            return Result.success(isFavorite ? "收藏成功" : "取消收藏成功", result);
        } catch (Exception e) {
            return Result.error("操作失败: " + e.getMessage());
        }
    }

    @PostMapping
    public Result<Void> addFavorite(
            @RequestBody Map<String, String> requestBody,
            HttpServletRequest request) {
        try {
            String userId = getUserIdFromRequest(request);
            String productId = requestBody.get("productId");
            boolean success = favoriteService.addFavorite(userId, productId);
            if (success) {
                return Result.success("收藏成功", null);
            }
            return Result.error("收藏失败");
        } catch (Exception e) {
            return Result.error("收藏失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{productId}")
    public Result<Void> removeFavorite(
            @PathVariable String productId,
            HttpServletRequest request) {
        try {
            String userId = getUserIdFromRequest(request);
            boolean success = favoriteService.removeFavorite(userId, productId);
            if (success) {
                return Result.success("取消收藏成功", null);
            }
            return Result.error("取消收藏失败");
        } catch (Exception e) {
            return Result.error("取消收藏失败: " + e.getMessage());
        }
    }

    private String getUserIdFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            return jwtUtil.getUserIdFromToken(token);
        }
        throw new RuntimeException("未登录");
    }
}