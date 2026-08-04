package com.example.trendytoysocialecommercehd.controller;

import com.example.trendytoysocialecommercehd.common.Result;
import com.example.trendytoysocialecommercehd.entity.BlindBoxStorage;
import com.example.trendytoysocialecommercehd.entity.Order;
import com.example.trendytoysocialecommercehd.service.BlindBoxStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 盲盒暂存柜 Controller
 */
@Tag(name = "盲盒暂存柜")
@RestController
@RequestMapping("/api/blind-box/storage")
public class BlindBoxStorageController {

    @Autowired
    private BlindBoxStorageService blindBoxStorageService;

    @Autowired
    private com.example.trendytoysocialecommercehd.util.JwtUtil jwtUtil;

    private String getUserIdFromToken(String token) {
        if (token == null || token.isEmpty()) return null;
        String cleanToken = token.replace("Bearer ", "");
        if (!jwtUtil.validateToken(cleanToken)) return null;
        return jwtUtil.getUserIdFromToken(cleanToken);
    }

    @GetMapping("/my-items")
    @Operation(summary = "获取当前用户暂存柜物品（用于盒柜导入）")
    public Result<List<BlindBoxStorage>> myItems(@RequestHeader(value = "Authorization", required = false) String token) {
        try {
            String userId = getUserIdFromToken(token);
            if (userId == null) return Result.error("未登录");
            List<BlindBoxStorage> list = blindBoxStorageService.getUserStorage(userId, true);
            return Result.success(list);
        } catch (Exception e) {
            return Result.error("获取暂存柜失败: " + e.getMessage());
        }
    }

    @GetMapping
    @Operation(summary = "获取用户暂存柜列表")
    public Result<List<BlindBoxStorage>> list(
            @RequestParam String userId,
            @RequestParam(required = false, defaultValue = "true") boolean onlyStored) {
        try {
            List<BlindBoxStorage> list = blindBoxStorageService.getUserStorage(userId, onlyStored);
            return Result.success(list);
        } catch (Exception e) {
            return Result.error("获取暂存柜失败: " + e.getMessage());
        }
    }

    @GetMapping("/count")
    @Operation(summary = "获取用户暂存数量")
    public Result<Map<String, Integer>> count(@RequestParam String userId) {
        try {
            int count = blindBoxStorageService.getStoredCount(userId);
            return Result.success(Map.of("count", count));
        } catch (Exception e) {
            return Result.error("获取暂存数量失败: " + e.getMessage());
        }
    }

    @PostMapping("/{storageId}/ship")
    @Operation(summary = "暂存柜发货（生成订单）")
    public Result<Order> ship(
            @PathVariable String storageId,
            @RequestBody Map<String, String> body) {
        try {
            String userId = body.get("userId");
            String addressId = body.get("addressId");
            Order order = blindBoxStorageService.shipFromCabinet(storageId, userId, addressId);
            return Result.success(order);
        } catch (Exception e) {
            return Result.error("发货失败: " + e.getMessage());
        }
    }

    @PostMapping("/batch-ship")
    @Operation(summary = "批量发货")
    public Result<List<Order>> batchShip(@RequestBody Map<String, Object> body) {
        try {
            @SuppressWarnings("unchecked")
            List<String> storageIds = (List<String>) body.get("storageIds");
            String userId = (String) body.get("userId");
            String addressId = (String) body.get("addressId");
            List<Order> orders = blindBoxStorageService.batchShip(storageIds, userId, addressId);
            return Result.success(orders);
        } catch (Exception e) {
            return Result.error("批量发货失败: " + e.getMessage());
        }
    }
}
