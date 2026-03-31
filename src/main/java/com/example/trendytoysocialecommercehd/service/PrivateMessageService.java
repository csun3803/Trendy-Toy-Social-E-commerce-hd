package com.example.trendytoysocialecommercehd.service;

import com.example.trendytoysocialecommercehd.entity.PrivateMessage;

import java.util.List;

public interface PrivateMessageService {
    List<PrivateMessage> getMessagesByConversationId(String conversationId);
    PrivateMessage sendMessage(String conversationId, String senderId, String receiverId, String content, String messageType);
    void markMessageAsRead(String messageId);
    int getUnreadCount(String userId);
}
