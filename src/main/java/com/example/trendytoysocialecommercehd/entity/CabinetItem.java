package com.example.trendytoysocialecommercehd.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("cabinet_item")
public class CabinetItem {
    @TableId(type = IdType.INPUT)
    private String itemId;

    private String cabinetId;
    private String productId;
    private String customName;
    private String displayNote;
    private String acquisitionMethod;
    private LocalDate acquisitionDate;
    private BigDecimal acquisitionPrice;
    private String displayImage;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime addedAt;

    private String customTags;
    private String itemType;
    private Integer quantity;
    private String attributes;
    private String dimensions;

    @TableField(exist = false)
    private String productName;

    @TableField(exist = false)
    private String productImage;
}