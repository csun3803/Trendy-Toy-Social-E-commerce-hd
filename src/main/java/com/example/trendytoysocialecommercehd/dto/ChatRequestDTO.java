package com.example.trendytoysocialecommercehd.dto;

import lombok.Data;

@Data
public class ChatRequestDTO {

    private String userId;

    private String sessionId;

    private String message;
}
