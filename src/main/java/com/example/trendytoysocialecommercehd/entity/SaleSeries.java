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

    private Integer variantCount;

    @TableField(exist = false)
    private Integer totalSales;

    @TableField(exist = false)
    private Double minPrice;

    @TableField(exist = false)
    private Double maxPrice;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private Date createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private Date updatedAt;

    @TableField(exist = false)
    private String shopName;
}