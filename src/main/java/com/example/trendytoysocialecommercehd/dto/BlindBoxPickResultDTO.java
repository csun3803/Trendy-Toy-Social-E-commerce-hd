package com.example.trendytoysocialecommercehd.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 选盒购买结果DTO
 */
@Data
public class BlindBoxPickResultDTO {

    /** 关联的订单ID */
    private String orderId;

    /** 订单编号 */
    private String orderNo;

    /** 槽位编号 */
    private Integer slotNo;

    /** 槽位编码 */
    private String slotCode;

    /** 原始产品ID */
    private String variantId;

    /** 款式名称 */
    private String variantName;

    /** 款式图片 */
    private String variantImage;

    /** 是否隐藏款 */
    private Boolean isHidden;

    /** 是否保底 */
    private Boolean isGuaranteed;

    /** 单价 */
    private BigDecimal price;

    /** 总价 */
    private BigDecimal totalPrice;

    /** 暂存柜记录ID（抽中后自动存入暂存柜） */
    private String storageId;
}
