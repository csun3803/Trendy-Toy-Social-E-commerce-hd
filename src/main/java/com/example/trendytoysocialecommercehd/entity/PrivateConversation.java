package com.example.trendytoysocialecommercehd.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("private_conversation")
public class PrivateConversation {
    @TableId(type = IdType.ASSIGN_UUID)
    private String conversationId;
    private String userAId;
    private String userBId;
    private Date createdAt;
    private Date lastMessageAt;
    private Date userAReadAt;
    private Date userBReadAt;
    private String status;
}
