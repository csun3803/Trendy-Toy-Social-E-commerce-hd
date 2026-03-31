package com.example.trendytoysocialecommercehd.service.impl;

import com.example.trendytoysocialecommercehd.entity.PrivateMessage;
import com.example.trendytoysocialecommercehd.mapper.PrivateMessageMapper;
import com.example.trendytoysocialecommercehd.service.PrivateMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class PrivateMessageServiceImpl implements PrivateMessageService {
    @Autowired
    private PrivateMessageMapper messageMapper;

    @Override
    public List<PrivateMessage> getMessagesByConversationId(String conversationId) {
        return messageMapper.findMessagesByConversationId(conversationId);
    }

    @Override
    public PrivateMessage sendMessage(String conversationId, String senderId, String receiverId, String content, String messageType) {
        PrivateMessage message = new PrivateMessage();
        message.setConversationId(conversationId);
        message.setSenderId(senderId);
        message.setReceiverId(receiverId);
        message.setContent(content);
        message.setMessageType(messageType);
        message.setSentAt(new Date());
        message.setStatus("delivered");
        messageMapper.insert(message);
        return message;
    }

    @Override
    public void markMessageAsRead(String messageId) {
        PrivateMessage message = messageMapper.selectById(messageId);
        if (message != null) {
            message.setReadAt(new Date());
            message.setStatus("read");
            messageMapper.updateById(message);
        }
    }

    @Override
    public int getUnreadCount(String userId) {
        List<PrivateMessage> unreadMessages = messageMapper.findUnreadMessagesByUserId(userId);
        return unreadMessages.size();
    }
}
