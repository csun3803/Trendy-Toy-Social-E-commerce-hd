package com.example.trendytoysocialecommercehd.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("my_display_cabinet")
public class MyDisplayCabinet {
    @TableId(type = IdType.INPUT)
    private String cabinetId;

    private String userId;
    private String cabinetName;
    private String description;
    private Integer isPublic;
    private Integer totalItems;
    private BigDecimal totalValuation;
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private Integer shareCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private String coverImage;
}