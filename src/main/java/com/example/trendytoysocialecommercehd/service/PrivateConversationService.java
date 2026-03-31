package com.example.trendytoysocialecommercehd.service;

import com.example.trendytoysocialecommercehd.entity.PrivateConversation;

import java.util.List;

public interface PrivateConversationService {
    List<PrivateConversation> getUserConversations(String userId);
    PrivateConversation getConversationById(String conversationId);
    PrivateConversation createConversation(String userAId, String userBId);
    void markAsRead(String conversationId, String userId);
    void markAllAsRead(String userId);
}
