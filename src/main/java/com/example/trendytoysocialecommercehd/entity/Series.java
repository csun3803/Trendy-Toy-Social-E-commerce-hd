package com.example.trendytoysocialecommercehd.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;

@Data
@TableName("series")
public class Series {
    @TableId(value = "series_id", type = IdType.INPUT)
    private String seriesId;

    private String seriesName;

    @TableField("ip_album_id")
    private String ipAlbumId;

    private String theme;

    private Integer releaseYear;

    private String description;

    @TableField("cover_image")
    private String coverImage;

    private Integer regularVariants;

    private Integer hiddenVariants;

    private Integer totalVariants;

    @TableField("is_limited")
    private Boolean isLimited;

    @TableField("limited_quantity")
    private Integer limitedQuantity;

    private BigDecimal minPrice;

    @TableField("fullset_price")
    private BigDecimal fullsetPrice;

    private String status;

    @TableField("series_hotness")
    private Integer seriesHotness;

    @TableField("start_date")
    private LocalDate startDate;

    @TableField("end_date")
    private LocalDate endDate;

    // 销量字段（通过查询计算）
    @TableField(exist = false)
    private Integer salesCount;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}