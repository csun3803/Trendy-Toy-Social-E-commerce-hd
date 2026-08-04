package com.example.trendytoysocialecommercehd.dto;

import lombok.Data;

@Data
public class ChatRequestDTO {

    private String userId;

    private String sessionId;

    private String message;

    /** 统一会话管理的 sessionId（customer_service_session 表的主键），用于更新活跃时间 */
    private String csSessionId;
}
