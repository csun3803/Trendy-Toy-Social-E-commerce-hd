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

    /** 预分配的原始产品ID（图鉴款式ID） */
    private String variantId;

    /** 缓存的款式名称（创建盒子时从图鉴复制） */
    private String variantName;

    /** 缓存的款式图片（创建盒子时从图鉴复制） */
    private String variantImage;

    /** 款式类型: regular常规/hidden隐藏 */
    private String variantType;

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
}
