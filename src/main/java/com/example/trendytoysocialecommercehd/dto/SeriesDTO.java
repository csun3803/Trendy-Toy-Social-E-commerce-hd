package com.example.trendytoysocialecommercehd.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class SeriesDTO {
    private String seriesId;
    private String seriesName;
    private String ipAlbumId;
    private String ipName; // IP名称（通过关联查询获取）
    private String theme;
    private Integer releaseYear;
    private String description;
    private String coverImage;
    private Integer regularVariants;
    private Integer hiddenVariants;
    private Integer totalVariants;
    private Boolean limited;
    private Integer limitedQuantity;
    private BigDecimal minPrice;
    private BigDecimal fullsetPrice;
    private String status;
    private Integer seriesHotness;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer salesCount; // 销量
}