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

    private Integer totalSales;

    @TableField(fill = FieldFill.INSERT)
    private Date createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updatedAt;
}
