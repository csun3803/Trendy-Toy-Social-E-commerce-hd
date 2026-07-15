package com.example.trendytoysocialecommercehd.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 盲盒暂存柜实体
 */
@Data
@TableName("blind_box_storage")
public class BlindBoxStorage {

    @TableId(value = "storage_id", type = IdType.INPUT)
    private String storageId;

    private String userId;

    private String machineId;

    private String machineName;

    private String setId;

    private Integer slotNo;

    private String saleVariantId;

    private String variantId;

    private String variantName;

    private String variantImage;

    private Boolean isHidden;

    private BigDecimal drawPrice;

    private String payOrderId;

    /** 状态: STORED暂存中/SHIPPED已发货 */
    private String status;

    private String shipOrderId;

    private Date storedAt;

    private Date shippedAt;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private Date createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private Date updatedAt;
}
