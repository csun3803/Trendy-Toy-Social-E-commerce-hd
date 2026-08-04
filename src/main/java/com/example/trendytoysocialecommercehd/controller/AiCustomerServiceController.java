package com.example.trendytoysocialecommercehd.controller;

import com.example.trendytoysocialecommercehd.common.Result;
import com.example.trendytoysocialecommercehd.dto.ChatRequestDTO;
import com.example.trendytoysocialecommercehd.entity.ChatMessage;
import com.example.trendytoysocialecommercehd.entity.CustomerServiceSession;
import com.example.trendytoysocialecommercehd.service.AiCustomerService;
import com.example.trendytoysocialecommercehd.service.CustomerServiceSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai/customer-service")
@RequiredArgsConstructor
public class AiCustomerServiceController {

    private final AiCustomerService customerService;
    private final CustomerServiceSessionService sessionService;

    /**
     * 发送消息（智能客服对话）
     * 如果传了 csSessionId，则用对应的 aiSessionId 调用 Python 服务，并更新会话活跃时间
     */
    @PostMapping("/chat")
    public Result<ChatMessage> chat(@RequestBody ChatRequestDTO dto) {
        String sessionId = dto.getSessionId();
        String csSessionId = dto.getCsSessionId();

        // 如果传了 csSessionId，从 customer_service_session 中查出 aiSessionId
        if (csSessionId != null && !csSessionId.isEmpty()) {
            CustomerServiceSession csSession = sessionService.getSessionById(csSessionId);
            if (csSession != null && csSession.getAiSessionId() != null) {
                sessionId = csSession.getAiSessionId();
            }
        }

        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = java.util.UUID.randomUUID().toString();
        }
        String userId = dto.getUserId();
        if (userId == null || userId.isEmpty()) {
            userId = "anonymous";
        }
        String message = dto.getMessage();
        if (message == null) {
            message = "";
        }
        ChatMessage reply = customerService.chat(userId, sessionId, message);

        // 更新会话活跃时间
        if (csSessionId != null && !csSessionId.isEmpty()) {
            sessionService.updateActiveTime(csSessionId);
        }

        return Result.success(reply);
    }

    /**
     * 获取聊天历史
     */
    @GetMapping("/history")
    public Result<List<ChatMessage>> getChatHistory(
            @RequestParam String userId,
            @RequestParam String sessionId) {
        List<ChatMessage> history = customerService.getChatHistory(userId, sessionId);
        return Result.success(history);
    }

    /**
     * 获取用户的会话列表
     */
    @GetMapping("/sessions")
    public Result<List<String>> getSessions(@RequestParam String userId) {
        List<String> sessions = customerService.getSessionIds(userId);
        return Result.success(sessions);
    }
}
