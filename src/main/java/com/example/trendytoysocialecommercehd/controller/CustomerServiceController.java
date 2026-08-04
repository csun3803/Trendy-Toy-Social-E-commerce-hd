package com.example.trendytoysocialecommercehd.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.trendytoysocialecommercehd.common.Result;
import com.example.trendytoysocialecommercehd.entity.CustomerServiceMessage;
import com.example.trendytoysocialecommercehd.entity.CustomerServiceSession;
import com.example.trendytoysocialecommercehd.service.CustomerServiceSessionService;
import com.example.trendytoysocialecommercehd.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/customer-service")
public class CustomerServiceController {

    @Autowired
    private CustomerServiceSessionService sessionService;

    @Autowired
    private JwtUtil jwtUtil;

    // ========== 管理端接口 ==========

    /**
     * 获取会话列表
     */
    @GetMapping("/sessions")
    public Result<?> getSessionList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String mode) {
        try {
            Page<CustomerServiceSession> sessionPage = sessionService.getSessionList(page, size, status, source, mode);
            return Result.success(sessionPage);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取会话列表失败");
        }
    }

    /**
     * 获取会话详情
     */
    @GetMapping("/sessions/{sessionId}")
    public Result<?> getSessionDetail(@PathVariable String sessionId) {
        try {
            CustomerServiceSession session = sessionService.getSessionById(sessionId);
            if (session == null) {
                return Result.error("会话不存在");
            }
            // 标记消息为已读
            sessionService.markMessagesAsRead(sessionId);
            return Result.success(session);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取会话详情失败");
        }
    }

    /**
     * 获取会话的消息列表
     */
    @GetMapping("/sessions/{sessionId}/messages")
    public Result<?> getMessages(@PathVariable String sessionId) {
        try {
            List<CustomerServiceMessage> messages = sessionService.getMessagesBySessionId(sessionId);
            return Result.success(messages);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取消息列表失败");
        }
    }

    /**
     * 管理员回复消息
     */
    @PostMapping("/sessions/{sessionId}/reply")
    public Result<?> replyMessage(
            @PathVariable String sessionId,
            @RequestBody Map<String, String> body,
            @RequestHeader("Authorization") String token) {
        try {
            String cleanToken = token.replace("Bearer ", "");
            String adminId = jwtUtil.getUserIdFromToken(cleanToken);
            String content = body.get("content");

            if (content == null || content.trim().isEmpty()) {
                return Result.error("回复内容不能为空");
            }

            CustomerServiceMessage message = sessionService.replyMessage(sessionId, adminId, content);
            return Result.success(message);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("回复失败");
        }
    }

    /**
     * 更新会话状态
     */
    @PutMapping("/sessions/{sessionId}/status")
    public Result<?> updateSessionStatus(
            @PathVariable String sessionId,
            @RequestBody Map<String, String> body) {
        try {
            String status = body.get("status");
            if (status == null || status.isEmpty()) {
                return Result.error("状态不能为空");
            }
            sessionService.updateSessionStatus(sessionId, status);
            return Result.success("更新成功");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("更新状态失败");
        }
    }

    // ========== 用户端接口 ==========

    /**
     * 创建/获取AI模式会话
     * 如果有未超时的活跃会话则返回，否则创建新的AI会话
     */
    @PostMapping("/user/ai-session")
    public Result<?> createAiSession(@RequestBody Map<String, String> body) {
        try {
            String userId = body.get("userId");
            String userNickname = body.getOrDefault("userNickname", "");

            if (userId == null || userId.isEmpty()) {
                return Result.error("用户ID不能为空");
            }

            CustomerServiceSession session = sessionService.createAiSession(userId, userNickname);
            return Result.success(session);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("创建会话失败");
        }
    }

    /**
     * 转人工：AI模式切换为HUMAN模式（同一会话）
     */
    @PostMapping("/user/transfer-to-human")
    public Result<?> transferToHuman(@RequestBody Map<String, String> body) {
        try {
            String sessionId = body.get("sessionId");
            if (sessionId == null || sessionId.isEmpty()) {
                return Result.error("会话ID不能为空");
            }
            sessionService.transferToHuman(sessionId);
            return Result.success("转接成功");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("转接失败: " + e.getMessage());
        }
    }

    /**
     * 心跳：更新会话活跃时间
     */
    @PostMapping("/user/heartbeat")
    public Result<?> heartbeat(@RequestBody Map<String, String> body) {
        try {
            String sessionId = body.get("sessionId");
            if (sessionId == null || sessionId.isEmpty()) {
                return Result.error("会话ID不能为空");
            }
            sessionService.updateActiveTime(sessionId);
            return Result.success("ok");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("心跳失败");
        }
    }

    /**
     * 用户关闭会话
     */
    @PostMapping("/user/close-session")
    public Result<?> closeSession(@RequestBody Map<String, String> body) {
        try {
            String sessionId = body.get("sessionId");
            if (sessionId == null || sessionId.isEmpty()) {
                return Result.error("会话ID不能为空");
            }
            sessionService.closeSession(sessionId);
            return Result.success("会话已结束");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("关闭会话失败");
        }
    }

    /**
     * 用户创建/获取人工客服会话（兼容旧逻辑）
     */
    @PostMapping("/user/session")
    public Result<?> createUserSession(@RequestBody Map<String, String> body) {
        try {
            String userId = body.get("userId");
            String userNickname = body.getOrDefault("userNickname", "");
            String source = body.getOrDefault("source", "商品咨询");

            if (userId == null || userId.isEmpty()) {
                return Result.error("用户ID不能为空");
            }

            CustomerServiceSession session = sessionService.createSession(userId, userNickname, source);
            return Result.success(session);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("创建会话失败");
        }
    }

    /**
     * 用户发送消息
     */
    @PostMapping("/user/message")
    public Result<?> userSendMessage(@RequestBody Map<String, String> body) {
        try {
            String sessionId = body.get("sessionId");
            String userId = body.get("userId");
            String content = body.get("content");

            if (sessionId == null || userId == null || content == null || content.trim().isEmpty()) {
                return Result.error("参数不完整");
            }

            CustomerServiceMessage message = sessionService.userSendMessage(sessionId, userId, content);
            return Result.success(message);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("发送消息失败");
        }
    }

    /**
     * 用户获取进行中的会话（含超时检查）
     */
    @GetMapping("/user/active-session")
    public Result<?> getActiveSession(@RequestParam String userId) {
        try {
            CustomerServiceSession session = sessionService.getActiveSessionByUserId(userId);
            return Result.success(session);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取会话失败");
        }
    }

    /**
     * 用户获取会话消息列表
     */
    @GetMapping("/user/messages")
    public Result<?> getUserMessages(@RequestParam String sessionId) {
        try {
            List<CustomerServiceMessage> messages = sessionService.getMessagesBySessionId(sessionId);
            return Result.success(messages);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取消息列表失败");
        }
    }
}
