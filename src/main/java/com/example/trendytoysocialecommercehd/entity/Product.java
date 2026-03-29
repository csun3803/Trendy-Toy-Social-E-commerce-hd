package com.example.trendytoysocialecommercehd.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;

@Data
@TableName("product")
public class Product {
    @TableId(value = "product_id", type = IdType.INPUT)
    private String productId;

    private String name;
    private String description;
    private BigDecimal price;
    private Integer stock;
    private String category;
    private String brand;

    @TableField("series_id")
    private String seriesId;

    @TableField("series_order")
    private Integer seriesOrder;

    @TableField("is_hidden_variant")
    private Boolean hiddenVariant;

    private String status;

    @TableField("release_date")
    private LocalDate releaseDate;

    @TableField("authenticity_code_field")
    private String authenticityCode;

    private String dimensions;
    private String material;
    private String designer;

    @TableField("copyright_owner")
    private String copyrightOwner;

    private BigDecimal weight;

    @TableField("package_type")
    private String packageType;

    @TableField("suitable_age")
    private String suitableAge;

    private String origin;

    @TableField("market_hotness")
    private Integer marketHotness;

    @TableField("image_url")
    private String imageUrl;

    // 计算字段
    @TableField(exist = false)
    private String variantType; // regular/hidden
    @TableField(exist = false)
    private String rarity; // common/rare/secret

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

}