package com.example.trendytoysocialecommercehd.dto;

import lombok.Data;

import java.util.List;

/**
 * 发券请求：选择模板 + 选择用户（支持批量）
 */
@Data
public class IssueCouponRequest {
    private String templateId;
    private List<String> userIds;
}
