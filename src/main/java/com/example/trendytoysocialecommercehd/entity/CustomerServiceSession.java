package com.example.trendytoysocialecommercehd.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("customer_service_session")
public class CustomerServiceSession {
    @TableId(type = IdType.INPUT)
    private String sessionId;
    private String userId;
    private String userNickname;
    private String lastMessageContent;
    private Date lastMessageTime;
    private Integer unreadCount;
    private String status;
    private String mode;           // 会话模式: AI / HUMAN
    private Date lastActiveTime;   // 最后活跃时间（超时判断依据）
    private String aiSessionId;    // AI聊天会话ID（关联chat_message表）
    private String source;
    private String adminId;
    private Date createTime;
    private Date updateTime;
}
