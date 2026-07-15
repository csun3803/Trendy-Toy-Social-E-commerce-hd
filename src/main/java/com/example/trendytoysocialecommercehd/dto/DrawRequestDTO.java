package com.example.trendytoysocialecommercehd.dto;

import lombok.Data;

/**
 * 抽盒请求DTO
 */
@Data
public class DrawRequestDTO {

    /** 抽盒机ID */
    private String machineId;

    /** 用户ID */
    private String userId;

    /** 抽盒类型: SINGLE(单抽) / TEN(十连抽) */
    private String drawType;

    /** 收货地址ID */
    private String addressId;

    /** 支付方式: WECHAT / ALIPAY */
    private String paymentMethod;
}
