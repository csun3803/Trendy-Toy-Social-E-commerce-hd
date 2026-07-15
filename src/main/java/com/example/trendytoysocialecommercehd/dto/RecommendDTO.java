package com.example.trendytoysocialecommercehd.dto;

import lombok.Data;

import java.util.List;

@Data
public class RecommendDTO {

    private String userId;

    private Integer limit;

    /** 指定系列ID列表（用于相似推荐） */
    private List<String> seriesIds;
}
