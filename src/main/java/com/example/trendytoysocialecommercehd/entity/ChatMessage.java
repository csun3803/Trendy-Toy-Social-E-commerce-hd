package com.example.trendytoysocialecommercehd.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@TableName("chat_message")
public class ChatMessage {

    @TableId(type = IdType.ASSIGN_ID)
    private String messageId;

    private String userId;

    private String sessionId;

    /** user / assistant */
    private String role;

    private String content;

    private LocalDateTime createTime;

    /**
     * 结构化卡片数据（非数据库字段），用于前端渲染可点击的系列卡片。
     * 由 Python ai-service 在 Function Call 查询系列信息后回传，
     * 前端收到后渲染为卡片，点击跳转到 /series/{seriesId}。
     */
    @TableField(exist = false)
    private List<Map<String, Object>> cards;
}
