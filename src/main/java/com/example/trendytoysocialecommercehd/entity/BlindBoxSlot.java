package com.example.trendytoysocialecommercehd.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

/**
 * 盲盒槽位实体（九宫格选盒）
 */
@Data
@TableName("blind_box_slot")
public class BlindBoxSlot {

    @TableId(value = "slot_id", type = IdType.INPUT)
    private String slotId;

    /** 关联的抽盒机ID */
    private String machineId;

    /** 关联的套盒ID */
    private String setId;

    /** 槽位编号(1-9) */
    private Integer slotNo;

    /** 槽位编码 */
    private String slotCode;

    /** 状态: AVAILABLE/RESERVED/SOLD/SELECTED */
    private String status;

    /** 预分配的销售款式ID(选盒后揭晓) */
    private String saleVariantId;

    /** 预分配的原始产品ID */
    private String variantId;

    /** 是否为隐藏款(揭晓后填入) */
    private Boolean isHidden;

    /** 抽中者用户ID */
    private String drawnBy;

    /** 抽中时间 */
    private Date drawnAt;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private Date createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private Date updatedAt;

    /** 非数据库字段：款式名称 */
    @TableField(exist = false)
    private String variantName;

    /** 非数据库字段：款式图片 */
    @TableField(exist = false)
    private String variantImage;
}
