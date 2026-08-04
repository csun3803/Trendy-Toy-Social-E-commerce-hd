package com.example.trendytoysocialecommercehd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.trendytoysocialecommercehd.entity.CustomerServiceMessage;
import com.example.trendytoysocialecommercehd.entity.CustomerServiceSession;
import com.example.trendytoysocialecommercehd.entity.User;
import com.example.trendytoysocialecommercehd.mapper.CustomerServiceMessageMapper;
import com.example.trendytoysocialecommercehd.mapper.CustomerServiceSessionMapper;
import com.example.trendytoysocialecommercehd.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class CustomerServiceSessionService extends ServiceImpl<CustomerServiceSessionMapper, CustomerServiceSession> {

    @Autowired
    private CustomerServiceMessageMapper messageMapper;

    @Autowired
    private UserMapper userMapper;

    // AI模式超时时间：30分钟
    private static final long AI_TIMEOUT_MS = 30 * 60 * 1000L;
    // 人工模式超时时间：60分钟
    private static final long HUMAN_TIMEOUT_MS = 60 * 60 * 1000L;

    /**
     * 获取会话列表（分页+筛选+排序）
     * 排序：待处理 > 处理中 > 已关闭，同状态按最后消息时间倒序
     */
    public Page<CustomerServiceSession> getSessionList(int page, int size, String status, String source, String mode) {
        Page<CustomerServiceSession> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<CustomerServiceSession> wrapper = new LambdaQueryWrapper<>();

        if (status != null && !status.isEmpty()) {
            wrapper.eq(CustomerServiceSession::getStatus, status);
        }
        if (source != null && !source.isEmpty()) {
            wrapper.eq(CustomerServiceSession::getSource, source);
        }
        if (mode != null && !mode.isEmpty()) {
            wrapper.eq(CustomerServiceSession::getMode, mode);
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

        // 2. 更新会话：最后消息时间/内容，未读数归零，状态改为处理中，更新活跃时间
        CustomerServiceSession session = this.getById(sessionId);
        if (session != null) {
            session.setLastMessageContent(content);
            session.setLastMessageTime(new Date());
            session.setUnreadCount(0);
            if ("待处理".equals(session.getStatus())) {
                session.setStatus("处理中");
            }
            session.setAdminId(adminId);
            session.setLastActiveTime(new Date());
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
     * 用户创建人工客服会话（原有逻辑，兼容保留）
     * 如果 userNickname 为空，自动从用户表查询
     */
    @Transactional
    public CustomerServiceSession createSession(String userId, String userNickname, String source) {
        // 检查是否已有进行中的会话
        CustomerServiceSession existing = getActiveSessionByUserId(userId);
        if (existing != null) {
            return existing;
        }

        // 如果 userNickname 为空，从用户表查询
        String nickname = userNickname;
        if ((nickname == null || nickname.isEmpty()) && userId != null && !userId.isEmpty()) {
            try {
                User user = userMapper.selectById(userId);
                if (user != null) {
                    nickname = user.getUsername();
                }
            } catch (Exception e) {
                log.warn("查询用户昵称失败: userId={}, error={}", userId, e.getMessage());
            }
        }

        // 创建新会话
        CustomerServiceSession session = new CustomerServiceSession();
        session.setSessionId("cs_" + System.currentTimeMillis());
        session.setUserId(userId);
        session.setUserNickname(nickname);
        session.setStatus("待处理");
        session.setMode("HUMAN");
        session.setSource(source);
        session.setUnreadCount(0);
        session.setLastMessageTime(new Date());
        session.setLastActiveTime(new Date());
        session.setCreateTime(new Date());
        this.save(session);
        return session;
    }

    /**
     * 创建AI模式会话
     * 生成统一的 cs_xxx 作为 sessionId，同时生成 ai_session_id 关联 chat_message 表
     * 如果 userNickname 为空，自动从用户表查询
     */
    @Transactional
    public CustomerServiceSession createAiSession(String userId, String userNickname) {
        // 检查是否已有进行中的会话（含超时检查）
        CustomerServiceSession existing = getActiveSessionByUserId(userId);
        if (existing != null) {
            return existing;
        }

        // 如果 userNickname 为空，从用户表查询
        String nickname = userNickname;
        if ((nickname == null || nickname.isEmpty()) && userId != null && !userId.isEmpty()) {
            try {
                User user = userMapper.selectById(userId);
                if (user != null) {
                    nickname = user.getUsername();
                }
            } catch (Exception e) {
                log.warn("查询用户昵称失败: userId={}, error={}", userId, e.getMessage());
            }
        }

        // 创建新AI会话
        CustomerServiceSession session = new CustomerServiceSession();
        session.setSessionId("cs_" + System.currentTimeMillis());
        session.setUserId(userId);
        session.setUserNickname(nickname);
        session.setStatus("处理中");
        session.setMode("AI");
        session.setAiSessionId("session_" + UUID.randomUUID().toString().replace("-", ""));
        session.setSource("AI智能客服");
        session.setUnreadCount(0);
        session.setLastMessageTime(new Date());
        session.setLastActiveTime(new Date());
        session.setCreateTime(new Date());
        this.save(session);
        log.info("创建AI会话: sessionId={}, aiSessionId={}, userId={}, nickname={}", session.getSessionId(), session.getAiSessionId(), userId, nickname);
        return session;
    }

    /**
     * 获取用户的进行中会话（含超时检查）
     * AI模式30分钟无交互自动关闭，人工模式60分钟无交互自动关闭
     */
    public CustomerServiceSession getActiveSessionByUserId(String userId) {
        LambdaQueryWrapper<CustomerServiceSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomerServiceSession::getUserId, userId);
        wrapper.in(CustomerServiceSession::getStatus, "待处理", "处理中");
        wrapper.orderByDesc(CustomerServiceSession::getLastMessageTime);
        wrapper.last("LIMIT 1");
        CustomerServiceSession session = this.getOne(wrapper);

        if (session == null) {
            return null;
        }

        // 超时检查
        if (isSessionTimedOut(session)) {
            log.info("会话超时，自动关闭: sessionId={}, mode={}, lastActiveTime={}",
                    session.getSessionId(), session.getMode(), session.getLastActiveTime());
            session.setStatus("已关闭");
            this.updateById(session);
            return null;
        }

        return session;
    }

    /**
     * 判断会话是否超时
     */
    private boolean isSessionTimedOut(CustomerServiceSession session) {
        Date lastActive = session.getLastActiveTime();
        if (lastActive == null) {
            // 没有活跃时间记录，用最后消息时间或创建时间兜底
            lastActive = session.getLastMessageTime() != null ? session.getLastMessageTime() : session.getCreateTime();
        }
        if (lastActive == null) {
            return false;
        }

        long elapsed = System.currentTimeMillis() - lastActive.getTime();
        long timeoutMs = "HUMAN".equals(session.getMode()) ? HUMAN_TIMEOUT_MS : AI_TIMEOUT_MS;
        return elapsed > timeoutMs;
    }

    /**
     * 转人工：AI模式切换为HUMAN模式
     * 同一会话，不创建新会话
     * 支持传入 cs_session_id 或 ai_session_id
     */
    @Transactional
    public void transferToHuman(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            throw new RuntimeException("会话ID不能为空");
        }

        CustomerServiceSession session = this.getById(sessionId);

        // 如果按主键找不到，尝试通过 aiSessionId 查找（兼容前端传入 AI sessionId 的情况）
        if (session == null) {
            LambdaQueryWrapper<CustomerServiceSession> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(CustomerServiceSession::getAiSessionId, sessionId);
            wrapper.last("LIMIT 1");
            session = this.getOne(wrapper);
            log.info("按 aiSessionId 查找会话: aiSessionId={}, found={}", sessionId, session != null);
        }

        if (session == null) {
            throw new RuntimeException("会话不存在");
        }
        if ("已关闭".equals(session.getStatus())) {
            throw new RuntimeException("会话已关闭");
        }
        if ("HUMAN".equals(session.getMode())) {
            // 已经是人工模式，无需转换
            return;
        }

        // AI → HUMAN 模式切换
        session.setMode("HUMAN");
        session.setStatus("待处理");  // 等待人工客服接入
        session.setLastActiveTime(new Date());
        this.updateById(session);
        log.info("转人工: sessionId={}, aiSessionId={}", session.getSessionId(), session.getAiSessionId());
    }

    /**
     * 更新会话活跃时间（心跳/发消息时调用）
     */
    public void updateActiveTime(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            return;
        }
        CustomerServiceSession session = this.getById(sessionId);
        if (session != null && !"已关闭".equals(session.getStatus())) {
            session.setLastActiveTime(new Date());
            this.updateById(session);
        }
    }

    /**
     * 关闭会话
     */
    public void closeSession(String sessionId) {
        CustomerServiceSession session = this.getById(sessionId);
        if (session != null) {
            session.setStatus("已关闭");
            this.updateById(session);
            log.info("关闭会话: sessionId={}, mode={}", session.getSessionId(), session.getMode());
        }
    }

    /**
     * 用户发送消息（更新活跃时间）
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

        // 2. 更新会话：最后消息时间和内容，未读数+1，更新活跃时间
        CustomerServiceSession session = this.getById(sessionId);
        if (session != null) {
            session.setLastMessageContent(content);
            session.setLastMessageTime(new Date());
            session.setLastActiveTime(new Date());
            session.setUnreadCount(session.getUnreadCount() + 1);
            // 如果会话已关闭，重新打开
            if ("已关闭".equals(session.getStatus())) {
                session.setStatus("待处理");
            }
            this.updateById(session);
        }

        return message;
    }
}
