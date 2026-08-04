package com.example.trendytoysocialecommercehd.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.trendytoysocialecommercehd.common.Result;
import com.example.trendytoysocialecommercehd.entity.SystemNotification;
import com.example.trendytoysocialecommercehd.mapper.SystemNotificationMapper;
import com.example.trendytoysocialecommercehd.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 系统通知 Controller
 * 用户端路径: /api/notifications
 * 管理端路径: /api/admin/notifications
 */
@RestController
@Tag(name = "系统通知", description = "系统通知的发送与查询")
public class SystemNotificationController {

    @Autowired
    private SystemNotificationMapper systemNotificationMapper;

    @Autowired
    private JwtUtil jwtUtil;

    private String getUserIdFromToken(String token) {
        if (token == null || token.isEmpty()) {
            throw new RuntimeException("未登录");
        }
        String cleanToken = token.replace("Bearer ", "");
        if (!jwtUtil.validateToken(cleanToken)) {
            throw new RuntimeException("无效的token");
        }
        return jwtUtil.getUserIdFromToken(cleanToken);
    }

    // ==================== 用户端 ====================

    @GetMapping("/api/notifications/list")
    @Operation(summary = "用户端-获取系统通知列表（分页）")
    public Result<Page<SystemNotification>> list(
            @RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String category) {
        try {
            String userId = getUserIdFromToken(token);
            Page<SystemNotification> pageParam = new Page<>(page, size);
            LambdaQueryWrapper<SystemNotification> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SystemNotification::getUserId, userId);
            if (category != null && !category.isEmpty()) {
                wrapper.eq(SystemNotification::getCategory, category);
            }
            wrapper.orderByDesc(SystemNotification::getCreatedAt);
            Page<SystemNotification> result = systemNotificationMapper.selectPage(pageParam, wrapper);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("获取通知列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/api/notifications/unread-count")
    @Operation(summary = "用户端-获取未读通知数量")
    public Result<Map<String, Object>> unreadCount(@RequestHeader("Authorization") String token) {
        try {
            String userId = getUserIdFromToken(token);
            int count = systemNotificationMapper.countUnreadByUserId(userId);
            // 同时获取最新一条通知的时间
            SystemNotification latest = systemNotificationMapper.selectLatestByUserId(userId);
            Map<String, Object> data = new HashMap<>();
            data.put("unreadCount", count);
            data.put("latestTime", latest != null ? latest.getCreatedAt() : null);
            return Result.success(data);
        } catch (Exception e) {
            return Result.error("获取未读数量失败: " + e.getMessage());
        }
    }

    @PutMapping("/api/notifications/{notificationId}/read")
    @Operation(summary = "用户端-标记单条通知已读")
    public Result<Void> markAsRead(
            @RequestHeader("Authorization") String token,
            @PathVariable String notificationId) {
        try {
            String userId = getUserIdFromToken(token);
            SystemNotification notification = systemNotificationMapper.selectById(notificationId);
            if (notification == null || !notification.getUserId().equals(userId)) {
                return Result.error("通知不存在");
            }
            if (!notification.getIsRead()) {
                notification.setIsRead(true);
                notification.setReadAt(new Date());
                systemNotificationMapper.updateById(notification);
            }
            return Result.success();
        } catch (Exception e) {
            return Result.error("标记已读失败: " + e.getMessage());
        }
    }

    @PutMapping("/api/notifications/read-all")
    @Operation(summary = "用户端-标记所有通知已读")
    public Result<Void> markAllAsRead(@RequestHeader("Authorization") String token) {
        try {
            String userId = getUserIdFromToken(token);
            systemNotificationMapper.markAllAsRead(userId);
            return Result.success();
        } catch (Exception e) {
            return Result.error("标记已读失败: " + e.getMessage());
        }
    }

    // ==================== 管理端/内部：创建系统通知 ====================

    @PostMapping("/api/admin/notifications/send")
    @Operation(summary = "管理端-发送系统通知给指定用户")
    public Result<Void> sendNotification(@RequestBody Map<String, Object> body) {
        try {
            String userId = (String) body.get("userId");
            String title = (String) body.get("title");
            String content = (String) body.get("content");
            String category = (String) body.get("category");
            String relatedId = (String) body.get("relatedId");
            String relatedType = (String) body.get("relatedType");

            if (userId == null || title == null) {
                return Result.error("用户ID和标题不能为空");
            }

            SystemNotification notification = new SystemNotification();
            notification.setNotificationId(UUID.randomUUID().toString());
            notification.setUserId(userId);
            notification.setTitle(title);
            notification.setContent(content);
            notification.setCategory(category != null ? category : "SYSTEM");
            notification.setRelatedId(relatedId);
            notification.setRelatedType(relatedType);
            notification.setIsRead(false);
            notification.setCreatedAt(new Date());

            systemNotificationMapper.insert(notification);
            return Result.success();
        } catch (Exception e) {
            return Result.error("发送通知失败: " + e.getMessage());
        }
    }

    @PostMapping("/api/admin/notifications/batch-send")
    @Operation(summary = "管理端-批量发送系统通知给多个用户")
    public Result<Void> batchSendNotification(@RequestBody Map<String, Object> body) {
        try {
            List<String> userIds = (List<String>) body.get("userIds");
            String title = (String) body.get("title");
            String content = (String) body.get("content");
            String category = (String) body.get("category");
            String relatedId = (String) body.get("relatedId");
            String relatedType = (String) body.get("relatedType");

            if (userIds == null || userIds.isEmpty() || title == null) {
                return Result.error("用户列表和标题不能为空");
            }

            for (String userId : userIds) {
                SystemNotification notification = new SystemNotification();
                notification.setNotificationId(UUID.randomUUID().toString());
                notification.setUserId(userId);
                notification.setTitle(title);
                notification.setContent(content);
                notification.setCategory(category != null ? category : "SYSTEM");
                notification.setRelatedId(relatedId);
                notification.setRelatedType(relatedType);
                notification.setIsRead(false);
                notification.setCreatedAt(new Date());
                systemNotificationMapper.insert(notification);
            }
            return Result.success();
        } catch (Exception e) {
            return Result.error("批量发送通知失败: " + e.getMessage());
        }
    }
}
