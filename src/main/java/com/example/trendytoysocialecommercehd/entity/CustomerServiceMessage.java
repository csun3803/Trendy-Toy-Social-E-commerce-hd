package com.example.trendytoysocialecommercehd.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("customer_service_message")
public class CustomerServiceMessage {
    @TableId(type = IdType.INPUT)
    private String messageId;
    private String sessionId;
    private String senderType;
    private String senderId;
    private String content;
    private String messageType;
    private Integer isRead;
    private Date createTime;
}
