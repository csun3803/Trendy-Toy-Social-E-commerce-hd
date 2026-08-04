package com.example.trendytoysocialecommercehd.dto;

import lombok.Data;

@Data
public class ShopUpdateDTO {
    private String category; // "qualification" | "identity" | "contact" | "config" | "finance"

    // Qualification fields (only updateable before approval)
    private Integer subjectType;
    private String legalPersonName; // 法人姓名 (个体户/企业必填)
    private String legalPersonIdCardNumber; // 法人身份证号码
    private String legalPersonIdCardExpiry; // 法人身份证有效期
    private String unifiedSocialCreditCode;

    // Brand fields
    private Integer hasBrand;
    private String brandAuthorizationLetter;
    private String trademarkRegistrationCert;

    // Identity fields (update goes to pending_data, needs audit)
    private String shopName;
    private String licenseImage;
    private String idCardFront;
    private String idCardBack;
    private String businessLicenseExpiry;

    // Contact fields (directly updateable)
    private String contactPerson;
    private String contactPhone;
    private String contactEmail;
    private String mainCategories;

    // Config fields (directly updateable)
    private String shopCover;
    private String shopIntro;
    private String customerServicePhone;
    private String customerServiceEmail;
    private String returnAddressProvince;
    private String returnAddressCity;
    private String returnAddressDistrict;
    private String returnAddressDetail;
    private String returnAddressContact;
    private String returnAddressPhone;

    // Finance fields (directly updateable, stored in shop_finance)
    private String bankName;
    private String bankAccount;
    private String accountHolder;
    private String branchName;
}
