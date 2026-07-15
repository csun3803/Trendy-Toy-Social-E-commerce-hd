package com.example.trendytoysocialecommercehd.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 抽盒记录实体
 */
@Data
@TableName("blind_box_draw_record")
public class BlindBoxDrawRecord {

    @TableId(value = "record_id", type = IdType.INPUT)
    private String recordId;

    /** 抽盒机ID */
    private String machineId;

    /** 用户ID */
    private String userId;

    /** 套盒ID */
    private String setId;

    /** 盒位号 */
    private Integer slotNo;

    /** 关联的销售款式ID（抽中的款式） */
    private String saleVariantId;

    /** 关联的原始产品ID */
    private String variantId;

    /** 抽盒类型: SINGLE单抽/TEN十连/PICK选盒 */
    private String drawType;

    /** 关联的订单ID */
    private String orderId;

    /** 关联的订单号（冗余字段，便于展示） */
    private String orderNo;

    /** 是否为隐藏款 */
    private Boolean isHidden;

    /** 是否触发保底 */
    private Boolean isGuaranteed;

    /** 抽盒时价格 */
    private BigDecimal drawPrice;

    /** 状态: PENDING_OPEN待开盒 / OPENED已开盒 */
    private String status;

    /** 开盒时间 */
    private Date openedAt;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private Date createdAt;

    /** 非数据库字段：款式名称（联表查询填充） */
    @TableField(exist = false)
    private String variantName;

    /** 非数据库字段：款式图片（联表查询填充） */
    @TableField(exist = false)
    private String variantImage;

    /** 非数据库字段：抽盒机名称（联表查询填充） */
    @TableField(exist = false)
    private String machineName;

    /** 非数据库字段：用户昵称（联表查询填充） */
    @TableField(exist = false)
    private String username;

    /** 非数据库字段：用户头像（联表查询填充） */
    @TableField(exist = false)
    private String avatarUrl;
}
