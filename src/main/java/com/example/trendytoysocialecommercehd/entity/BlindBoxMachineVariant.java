package com.example.trendytoysocialecommercehd.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 抽盒机款式覆盖配置实体
 * 用于商家为单个抽盒机覆盖 sale_variant 的库存和概率（默认复用商城数据）
 */
@Data
@TableName("blind_box_machine_variant")
public class BlindBoxMachineVariant {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;

    /** 抽盒机ID */
    private String machineId;

    /** 销售款式ID */
    private String saleVariantId;

    /** 是否覆盖库存: false否/true是 */
    private Boolean overrideStock;

    /** 覆盖的库存数量（overrideStock=true时生效） */
    private Integer stockQuantity;

    /** 是否覆盖概率: false否/true是 */
    private Boolean overrideProbability;

    /** 覆盖的抽出概率（0-1，overrideProbability=true时生效） */
    private BigDecimal drawProbability;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private Date createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private Date updatedAt;

    /** 非数据库字段：款式名称（联表查询填充） */
    @TableField(exist = false)
    private String variantName;

    /** 非数据库字段：款式图片（联表查询填充） */
    @TableField(exist = false)
    private String variantImage;

    /** 非数据库字段：原始产品ID（联表查询填充） */
    @TableField(exist = false)
    private String variantId;

    /** 非数据库字段：是否为隐藏款（联表查询填充） */
    @TableField(exist = false)
    private Boolean isHidden;

    /** 非数据库字段：原始库存（sale_variant 中的库存，用于默认值展示） */
    @TableField(exist = false)
    private Integer originalStock;

    /** 非数据库字段：抽出统计次数（数据页用） */
    @TableField(exist = false)
    private Integer drawCount;
}
