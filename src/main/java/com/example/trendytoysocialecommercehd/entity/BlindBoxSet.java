package com.example.trendytoysocialecommercehd.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 抽盒机套盒实体
 * 一个抽盒机有多套盒，每套盒有固定格数（默认3x3=9格）
 */
@Data
@TableName("blind_box_set")
public class BlindBoxSet {

    @TableId(value = "set_id", type = IdType.INPUT)
    private String setId;

    /** 关联的抽盒机ID */
    private String machineId;

    /** 套盒序号（用于排序和左右切换） */
    private Integer setIndex;

    /** 套盒名称（如：第1套） */
    private String setName;

    /** 盒位图URL */
    private String layoutImage;

    /** 行数 */
    private Integer gridRows;

    /** 列数 */
    private Integer gridCols;

    /** 总格数 */
    private Integer totalSlots;

    /** 已售格数 */
    private Integer soldCount;

    /** 状态: ACTIVE活跃/COMPLETED已售完 */
    private String status;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private Date createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private Date updatedAt;

    /** 非数据库字段：套盒下的格位列表 */
    @TableField(exist = false)
    private List<BlindBoxSlot> slots;
}
