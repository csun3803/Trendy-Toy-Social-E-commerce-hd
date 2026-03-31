package com.example.trendytoysocialecommercehd.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.trendytoysocialecommercehd.entity.PrivateConversation;
import com.example.trendytoysocialecommercehd.mapper.PrivateConversationMapper;
import com.example.trendytoysocialecommercehd.service.PrivateConversationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class PrivateConversationServiceImpl implements PrivateConversationService {
    @Autowired
    private PrivateConversationMapper conversationMapper;

    @Override
    public List<PrivateConversation> getUserConversations(String userId) {
        return conversationMapper.findConversationsByUserId(userId);
    }

    @Override
    public PrivateConversation getConversationById(String conversationId) {
        return conversationMapper.selectById(conversationId);
    }

    @Override
    public PrivateConversation createConversation(String userAId, String userBId) {
        QueryWrapper<PrivateConversation> wrapper = new QueryWrapper<>();
        wrapper.or(w -> w.eq("user_a_id", userAId).eq("user_b_id", userBId))
                .or(w -> w.eq("user_a_id", userBId).eq("user_b_id", userAId));
        PrivateConversation existing = conversationMapper.selectOne(wrapper);
        if (existing != null) {
            return existing;
        }

        PrivateConversation conversation = new PrivateConversation();
        conversation.setUserAId(userAId);
        conversation.setUserBId(userBId);
        conversation.setCreatedAt(new Date());
        conversation.setLastMessageAt(new Date());
        conversation.setStatus("active");
        conversationMapper.insert(conversation);
        return conversation;
    }

    @Override
    public void markAsRead(String conversationId, String userId) {
        PrivateConversation conversation = conversationMapper.selectById(conversationId);
        if (conversation != null) {
            if (userId.equals(conversation.getUserAId())) {
                conversation.setUserAReadAt(new Date());
            } else if (userId.equals(conversation.getUserBId())) {
                conversation.setUserBReadAt(new Date());
            }
            conversationMapper.updateById(conversation);
        }
    }

    @Override
    public void markAllAsRead(String userId) {
        List<PrivateConversation> conversations = conversationMapper.findConversationsByUserId(userId);
        for (PrivateConversation conversation : conversations) {
            markAsRead(conversation.getConversationId(), userId);
        }
    }
}
