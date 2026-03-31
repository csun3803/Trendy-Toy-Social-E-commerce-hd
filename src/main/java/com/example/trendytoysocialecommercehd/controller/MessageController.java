package com.example.trendytoysocialecommercehd.controller;

import com.example.trendytoysocialecommercehd.common.Result;
import com.example.trendytoysocialecommercehd.entity.PrivateConversation;
import com.example.trendytoysocialecommercehd.entity.PrivateMessage;
import com.example.trendytoysocialecommercehd.service.PrivateConversationService;
import com.example.trendytoysocialecommercehd.service.PrivateMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
public class MessageController {
    @Autowired
    private PrivateConversationService conversationService;
    @Autowired
    private PrivateMessageService messageService;

    @GetMapping("/conversations")
    public Result<List<PrivateConversation>> getConversations(@RequestParam String userId) {
        List<PrivateConversation> conversations = conversationService.getUserConversations(userId);
        return Result.success(conversations);
    }

    @GetMapping("/conversations/{id}")
    public Result<PrivateConversation> getConversation(@PathVariable String id) {
        PrivateConversation conversation = conversationService.getConversationById(id);
        return Result.success(conversation);
    }

    @PostMapping("/conversations")
    public Result<PrivateConversation> createConversation(@RequestParam String userAId, @RequestParam String userBId) {
        PrivateConversation conversation = conversationService.createConversation(userAId, userBId);
        return Result.success(conversation);
    }

    @PostMapping("/conversations/{id}/read")
    public Result<Void> markConversationAsRead(@PathVariable String id, @RequestParam String userId) {
        conversationService.markAsRead(id, userId);
        return Result.success();
    }

    @PostMapping("/read-all")
    public Result<Void> markAllAsRead(@RequestParam String userId) {
        conversationService.markAllAsRead(userId);
        return Result.success();
    }

    @GetMapping("/conversations/{id}/messages")
    public Result<List<PrivateMessage>> getMessages(@PathVariable String id) {
        List<PrivateMessage> messages = messageService.getMessagesByConversationId(id);
        return Result.success(messages);
    }

    @PostMapping("/messages")
    public Result<PrivateMessage> sendMessage(@RequestParam String conversationId, @RequestParam String senderId, @RequestParam String receiverId, @RequestParam String content, @RequestParam String messageType) {
        PrivateMessage message = messageService.sendMessage(conversationId, senderId, receiverId, content, messageType);
        return Result.success(message);
    }

    @PostMapping("/messages/{id}/read")
    public Result<Void> markMessageAsRead(@PathVariable String id) {
        messageService.markMessageAsRead(id);
        return Result.success();
    }

    @GetMapping("/unread-count")
    public Result<Integer> getUnreadCount(@RequestParam String userId) {
        int count = messageService.getUnreadCount(userId);
        return Result.success(count);
    }
}
