package com.example.trendytoysocialecommercehd.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

@Data
@TableName("activity_product_reference")
public class ActivityProductReference {
    @TableId(type = IdType.ASSIGN_UUID)
    private String referenceId;

    private String activityId;

    private String seriesId;

    @TableField(exist = false)
    private Series series;

    @TableField(exist = false)
    private SocialActivity activity;
}