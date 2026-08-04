package com.example.trendytoysocialecommercehd.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

@Data
@TableName("sale_series")
public class SaleSeries {
    @TableId(value = "sale_series_id", type = IdType.INPUT)
    private String saleSeriesId;

    private String shopId;

    private String seriesId;

    private String saleTitle;

    private String saleDescription;

    private String saleCoverImage;

    private String saleStatus;

    // 商家上传的额外图片，JSON数组格式
    private String customImages;

    // 款式数量改为从sale_variant表实时统计
    @TableField(exist = false)
    private Integer variantCount;

    @TableField(exist = false)
    private Integer totalSales;

    @TableField(exist = false)
    private Double minPrice;

    @TableField(exist = false)
    private Double maxPrice;

    // 从 series 表关联获取
    @TableField(exist = false)
    private String theme;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private Date createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private Date updatedAt;

    @TableField(exist = false)
    private String shopName;
}