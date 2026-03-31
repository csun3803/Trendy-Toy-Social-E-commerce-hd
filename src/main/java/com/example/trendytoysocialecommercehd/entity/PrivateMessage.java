package com.example.trendytoysocialecommercehd.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("private_message")
public class PrivateMessage {
    @TableId(type = IdType.ASSIGN_UUID)
    private String messageId;
    private String conversationId;
    private String senderId;
    private String receiverId;
    private String content;
    private String messageType;
    private Date sentAt;
    private Date deliveredAt;
    private Date readAt;
    private String status;
    private String productId;
}
