package com.example.trendytoysocialecommercehd.dto;

import lombok.Data;

/**
 * 选盒购买请求DTO
 */
@Data
public class BlindBoxPickRequestDTO {

    /** 抽盒机ID */
    private String machineId;

    /** 套盒ID（用于精确查找槽位） */
    private String setId;

    /** 用户ID */
    private String userId;

    /** 槽位编号(1-9) */
    private Integer slotNo;

    /** 槽位编码 */
    private String slotCode;

    /** 收货地址ID */
    private String addressId;

    /** 支付方式: WECHAT / ALIPAY */
    private String paymentMethod;
}
