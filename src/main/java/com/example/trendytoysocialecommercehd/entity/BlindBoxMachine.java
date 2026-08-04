package com.example.trendytoysocialecommercehd.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 抽盒机实体
 */
@Data
@TableName("blind_box_machine")
public class BlindBoxMachine {

    @TableId(value = "machine_id", type = IdType.INPUT)
    private String machineId;

    /** 关联的图鉴系列ID（抽盒机独立引用图鉴，不复用商城数据） */
    @TableField(value = "series_id")
    private String seriesId;

    /** 套数（商家配置，如10套） */
    @TableField(value = "set_count", insertStrategy = FieldStrategy.ALWAYS)
    private Integer setCount;

    /** 隐藏款总数量（商家配置，如10套中放1个隐藏款） */
    @TableField(value = "hidden_count", insertStrategy = FieldStrategy.ALWAYS)
    private Integer hiddenCount;

    /** 关联的店铺ID */
    @TableField(value = "shop_id", insertStrategy = FieldStrategy.ALWAYS)
    private String shopId;

    /** 抽盒机名称 */
    @TableField(value = "machine_name", insertStrategy = FieldStrategy.ALWAYS)
    private String machineName;

    /** 抽盒机描述 */
    @TableField("machine_description")
    private String machineDescription;

    /** 抽盒机封面图 */
    @TableField("machine_cover_image")
    private String machineCoverImage;

    /** 单次抽盒价格 */
    @TableField(value = "draw_price", insertStrategy = FieldStrategy.ALWAYS)
    private BigDecimal drawPrice;

    /** 十连抽价格 */
    @TableField("ten_draw_price")
    private BigDecimal tenDrawPrice;

    /** 运行状态: ACTIVE启用/INACTIVE停用/TAKEDOWN强制下架 */
    @TableField("machine_status")
    private String machineStatus;

    /** 审核状态: DRAFT草稿/PENDING待审核/APPROVED已通过/REJECTED已驳回 */
    @TableField("audit_status")
    private String auditStatus;

    /** 审核备注（驳回原因/下架原因） */
    @TableField("audit_remark")
    private String auditRemark;

    /** 最近审核时间 */
    @TableField("audited_at")
    private Date auditedAt;

    /** 总库存（所有款式库存之和） */
    @TableField("total_stock")
    private Integer totalStock;

    /** 已抽取次数 */
    @TableField("total_draws")
    private Integer totalDraws;

    /** 累计流水 */
    @TableField("total_revenue")
    private BigDecimal totalRevenue;

    /** 保底次数（0表示无保底） */
    @TableField("guarantee_draws")
    private Integer guaranteeDraws;

    /** 排序权重 */
    @TableField("sort_order")
    private Integer sortOrder;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private Date createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private Date updatedAt;

    /** 非数据库字段：店铺名称 */
    @TableField(exist = false)
    private String shopName;

    /** 非数据库字段：图鉴系列名称 */
    @TableField(exist = false)
    private String seriesName;
}
