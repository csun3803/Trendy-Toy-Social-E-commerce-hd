package com.example.trendytoysocialecommercehd.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.trendytoysocialecommercehd.config.AiServiceClient;
import com.example.trendytoysocialecommercehd.entity.ChatMessage;
import com.example.trendytoysocialecommercehd.mapper.ChatMessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 智能客服服务
 * 优先调用独立 Python ai-service（智谱ChatGLM + RAG增强）
 * Python不可用时，回退到本地FAQ关键词匹配，保证可用性
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiCustomerService {

    private final ChatMessageMapper chatMessageMapper;
    private final AiServiceClient aiServiceClient;

    // 本地FAQ知识库（Python服务不可用时的兜底）
    private static final List<FaqItem> FAQ_KB = List.of(
            new FaqItem(List.of("退款", "退货", "退钱", "退换货"), "关于退款/退货：\n1. 在订单详情中点击'申请售后'\n2. 选择退款/退货原因并提交\n3. 商家审核通过后，退款将在3-5个工作日内原路返回\n4. 退货商品需保持未拆封状态"),
            new FaqItem(List.of("发货", "快递", "物流", "配送", "多久到"), "关于发货/物流：\n1. 商家一般会在下单后48小时内发货\n2. 发货后可在订单详情中查看物流信息\n3. 一般3-7天可送达，偏远地区可能需要更长时间"),
            new FaqItem(List.of("盲盒", "概率", "隐藏款", "抽盒"), "关于盲盒/抽盒：\n1. 盲盒为随机抽取，每个款式概率不同\n2. 常规款概率均等，隐藏款概率通常为1/144\n3. 整盒购买可集齐所有常规款"),
            new FaqItem(List.of("支付", "付款", "微信", "支付宝"), "关于支付方式：\n目前支持微信支付、支付宝等主流支付方式，下单后选择您方便的支付方式即可完成付款。"),
            new FaqItem(List.of("优惠券", "折扣", "满减", "活动"), "关于优惠活动：\n1. 优惠券可在'我的-优惠券'中查看\n2. 下单时选择可用优惠券即可抵扣\n3. 每单限用一张优惠券"),
            new FaqItem(List.of("售后", "投诉", "质量问题", "损坏"), "关于售后问题：\n1. 收到商品7天内可申请售后\n2. 质量问题请拍照留证后联系商家\n3. 运输损坏请在签收时拒收并联系客服"),
            new FaqItem(List.of("盒柜", "展示柜"), "关于盒柜功能：\n1. 盒柜用于展示您收藏的潮玩\n2. 可在'我的-我的盒柜'中添加和管理\n3. 支持创建多个主题柜")
    );

    private static final String DEFAULT_REPLY = "感谢您的咨询！我暂时无法理解您的问题，您可以尝试：\n1. 换个方式描述您的问题\n2. 选择以下常见问题：退款/退货、发货/物流、盲盒/抽盒、支付方式、优惠券、售后问题\n3. 如需人工客服，请拨打客服电话";

    /**
     * 处理用户消息并返回智能回复
     */
    public ChatMessage chat(String userId, String sessionId, String message) {
        // 1. 优先调用Python ai-service (智谱ChatGLM + RAG)
        try {
            Map<String, Object> resp = aiServiceClient.chat(userId, sessionId, message);
            if (resp != null) {
                // Python服务已经把消息存入chat_message表，直接构造返回对象
                ChatMessage msg = new ChatMessage();
                msg.setMessageId((String) resp.get("messageId"));
                msg.setUserId((String) resp.get("userId"));
                msg.setSessionId((String) resp.get("sessionId"));
                msg.setRole((String) resp.get("role"));
                msg.setContent((String) resp.get("content"));
                Object createTime = resp.get("createTime");
                if (createTime != null) {
                    try {
                        msg.setCreateTime(LocalDateTime.parse(createTime.toString().replace(" ", "T")));
                    } catch (Exception e) {
                        msg.setCreateTime(LocalDateTime.now());
                    }
                }
                // 转发结构化卡片数据（Function Call 查询系列信息后回传，供前端渲染可点击卡片）
                Object cardsObj = resp.get("cards");
                if (cardsObj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> cards = (List<Map<String, Object>>) cardsObj;
                    msg.setCards(cards);
                }
                return msg;
            }
        } catch (Exception e) {
            log.warn("Python ai-service 调用失败，回退到本地FAQ: {}", e.getMessage());
        }

        // 2. 回退：本地FAQ + 关键词匹配
        return localChat(userId, sessionId, message);
    }

    /**
     * 本地兜底客服逻辑
     */
    private ChatMessage localChat(String userId, String sessionId, String message) {
        // 保存用户消息
        ChatMessage userMsg = new ChatMessage();
        userMsg.setUserId(userId);
        userMsg.setSessionId(sessionId);
        userMsg.setRole("user");
        userMsg.setContent(message);
        userMsg.setCreateTime(LocalDateTime.now());
        chatMessageMapper.insert(userMsg);

        String reply = generateLocalReply(message);

        ChatMessage assistantMsg = new ChatMessage();
        assistantMsg.setUserId(userId);
        assistantMsg.setSessionId(sessionId);
        assistantMsg.setRole("assistant");
        assistantMsg.setContent(reply);
        assistantMsg.setCreateTime(LocalDateTime.now());
        chatMessageMapper.insert(assistantMsg);

        return assistantMsg;
    }

    private String generateLocalReply(String message) {
        if (message == null || message.trim().isEmpty()) {
            return "您好！请问有什么可以帮助您的？";
        }
        String lowerMsg = message.toLowerCase();
        FaqItem bestMatch = null;
        int maxScore = 0;
        for (FaqItem item : FAQ_KB) {
            int score = 0;
            for (String kw : item.keywords) {
                if (lowerMsg.contains(kw.toLowerCase())) score++;
            }
            if (score > maxScore) {
                maxScore = score;
                bestMatch = item;
            }
        }
        if (bestMatch != null && maxScore > 0) {
            return bestMatch.answer;
        }
        if (lowerMsg.matches(".*[你好hihello嗨].*")) {
            return "您好！欢迎来到潮玩社交电商平台！我是AI智能客服，可以帮您解答关于订单、发货、退款、盲盒等问题。";
        }
        return DEFAULT_REPLY;
    }

    /**
     * 获取聊天历史
     */
    public List<ChatMessage> getChatHistory(String userId, String sessionId) {
        QueryWrapper<ChatMessage> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).eq("session_id", sessionId)
                .orderByAsc("create_time").last("LIMIT 100");
        return chatMessageMapper.selectList(wrapper);
    }

    /**
     * 获取用户的会话列表
     */
    public List<String> getSessionIds(String userId) {
        QueryWrapper<ChatMessage> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).select("DISTINCT session_id").orderByDesc("create_time");
        return chatMessageMapper.selectList(wrapper).stream()
                .map(ChatMessage::getSessionId).distinct().collect(Collectors.toList());
    }

    private static class FaqItem {
        List<String> keywords;
        String answer;
        FaqItem(List<String> k, String a) { keywords = k; answer = a; }
    }
}
