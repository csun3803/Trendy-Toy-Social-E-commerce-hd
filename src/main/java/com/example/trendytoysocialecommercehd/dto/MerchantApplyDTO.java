package com.example.trendytoysocialecommercehd.dto;

import lombok.Data;

@Data
public class MerchantApplyDTO {
    private String mobile;
    private String password;
    private String shopName;
    private String contactName;
    private Integer subjectType; // 0个人 1个体户 2企业
    private String licenseNo;
    private String licenseImage;
    private String idCardNo;
    private String idCardFront;
    private String idCardBack;
    private String bankAccountName;
    private String bankName;
    private String bankCardNo;
}
