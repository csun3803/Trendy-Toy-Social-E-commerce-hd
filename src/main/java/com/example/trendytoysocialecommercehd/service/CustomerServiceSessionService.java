package com.example.trendytoysocialecommercehd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.trendytoysocialecommercehd.entity.CustomerServiceMessage;
import com.example.trendytoysocialecommercehd.entity.CustomerServiceSession;
import com.example.trendytoysocialecommercehd.mapper.CustomerServiceMessageMapper;
import com.example.trendytoysocialecommercehd.mapper.CustomerServiceSessionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class CustomerServiceSessionService extends ServiceImpl<CustomerServiceSessionMapper, CustomerServiceSession> {

    @Autowired
    private CustomerServiceMessageMapper messageMapper;

    /**
     * 获取会话列表（分页+筛选+排序）
     * 排序：待处理 > 处理中 > 已关闭，同状态按最后消息时间倒序
     */
    public Page<CustomerServiceSession> getSessionList(int page, int size, String status, String source) {
        Page<CustomerServiceSession> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<CustomerServiceSession> wrapper = new LambdaQueryWrapper<>();

        if (status != null && !status.isEmpty()) {
            wrapper.eq(CustomerServiceSession::getStatus, status);
        }
        if (source != null && !source.isEmpty()) {
            wrapper.eq(CustomerServiceSession::getSource, source);
        }

        // 使用 FIELD 排序实现自定义状态顺序
        wrapper.last("ORDER BY FIELD(status, '待处理', '处理中', '已关闭'), last_message_time DESC");

        return this.page(pageObj, wrapper);
    }

    /**
     * 获取会话详情
     */
    public CustomerServiceSession getSessionById(String sessionId) {
        return this.getById(sessionId);
    }

    /**
     * 获取会话的所有消息
     */
    public List<CustomerServiceMessage> getMessagesBySessionId(String sessionId) {
        LambdaQueryWrapper<CustomerServiceMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomerServiceMessage::getSessionId, sessionId);
        wrapper.orderByAsc(CustomerServiceMessage::getCreateTime);
        return messageMapper.selectList(wrapper);
    }

    /**
     * 管理员回复消息
     */
    @Transactional
    public CustomerServiceMessage replyMessage(String sessionId, String adminId, String content) {
        // 1. 创建消息
        CustomerServiceMessage message = new CustomerServiceMessage();
        message.setMessageId("msg_" + System.currentTimeMillis());
        message.setSessionId(sessionId);
        message.setSenderType("admin");
        message.setSenderId(adminId);
        message.setContent(content);
        message.setMessageType("text");
        message.setIsRead(1);
        message.setCreateTime(new Date());
        messageMapper.insert(message);

        // 2. 更新会话：最后消息时间/内容，未读数归零，状态改为处理中
        CustomerServiceSession session = this.getById(sessionId);
        if (session != null) {
            session.setLastMessageContent(content);
            session.setLastMessageTime(new Date());
            session.setUnreadCount(0);
            if ("待处理".equals(session.getStatus())) {
                session.setStatus("处理中");
            }
            session.setAdminId(adminId);
            this.updateById(session);
        }

        return message;
    }

    /**
     * 更新会话状态
     */
    public void updateSessionStatus(String sessionId, String status) {
        CustomerServiceSession session = this.getById(sessionId);
        if (session != null) {
            session.setStatus(status);
            this.updateById(session);
        }
    }

    /**
     * 标记会话消息为已读
     */
    public void markMessagesAsRead(String sessionId) {
        // 更新会话未读数为0
        CustomerServiceSession session = this.getById(sessionId);
        if (session != null) {
            session.setUnreadCount(0);
            this.updateById(session);
        }

        // 更新该会话下所有未读消息为已读
        LambdaQueryWrapper<CustomerServiceMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomerServiceMessage::getSessionId, sessionId);
        wrapper.eq(CustomerServiceMessage::getIsRead, 0);
        List<CustomerServiceMessage> unreadMessages = messageMapper.selectList(wrapper);
        for (CustomerServiceMessage msg : unreadMessages) {
            msg.setIsRead(1);
            messageMapper.updateById(msg);
        }
    }

    /**
     * 用户创建人工客服会话
     */
    @Transactional
    public CustomerServiceSession createSession(String userId, String userNickname, String source) {
        // 检查是否已有进行中的会话
        LambdaQueryWrapper<CustomerServiceSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomerServiceSession::getUserId, userId);
        wrapper.in(CustomerServiceSession::getStatus, "待处理", "处理中");
        wrapper.orderByDesc(CustomerServiceSession::getLastMessageTime);
        wrapper.last("LIMIT 1");
        CustomerServiceSession existing = this.getOne(wrapper);
        if (existing != null) {
            return existing;
        }

        // 创建新会话
        CustomerServiceSession session = new CustomerServiceSession();
        session.setSessionId("cs_" + System.currentTimeMillis());
        session.setUserId(userId);
        session.setUserNickname(userNickname);
        session.setStatus("待处理");
        session.setSource(source);
        session.setUnreadCount(0);
        session.setLastMessageTime(new Date());
        session.setCreateTime(new Date());
        this.save(session);
        return session;
    }

    /**
     * 用户发送消息
     */
    @Transactional
    public CustomerServiceMessage userSendMessage(String sessionId, String userId, String content) {
        // 1. 创建消息
        CustomerServiceMessage message = new CustomerServiceMessage();
        message.setMessageId("msg_" + System.currentTimeMillis());
        message.setSessionId(sessionId);
        message.setSenderType("user");
        message.setSenderId(userId);
        message.setContent(content);
        message.setMessageType("text");
        message.setIsRead(0);
        message.setCreateTime(new Date());
        messageMapper.insert(message);

        // 2. 更新会话：最后消息时间和内容，未读数+1
        CustomerServiceSession session = this.getById(sessionId);
        if (session != null) {
            session.setLastMessageContent(content);
            session.setLastMessageTime(new Date());
            session.setUnreadCount(session.getUnreadCount() + 1);
            // 如果会话已关闭，重新打开
            if ("已关闭".equals(session.getStatus())) {
                session.setStatus("待处理");
            }
            this.updateById(session);
        }

        return message;
    }

    /**
     * 获取用户的进行中会话
     */
    public CustomerServiceSession getActiveSessionByUserId(String userId) {
        LambdaQueryWrapper<CustomerServiceSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomerServiceSession::getUserId, userId);
        wrapper.in(CustomerServiceSession::getStatus, "待处理", "处理中");
        wrapper.orderByDesc(CustomerServiceSession::getLastMessageTime);
        wrapper.last("LIMIT 1");
        return this.getOne(wrapper);
    }
}
