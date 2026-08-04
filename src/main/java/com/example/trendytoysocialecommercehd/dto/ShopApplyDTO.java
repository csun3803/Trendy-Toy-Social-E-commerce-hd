package com.example.trendytoysocialecommercehd.dto;

import lombok.Data;

import java.util.List;

@Data
public class ShopApplyDTO {
    // 资质类 (Qualification - locked after approval)
    private Integer subjectType; // 0个人 1个体户 2企业
    private String legalPersonName; // 法人姓名 (个体户/企业必填)
    private String legalPersonIdCardNumber; // 法人身份证号码 (原 idCardNumber)
    private String legalPersonIdCardExpiry; // 法人身份证有效期 (原 idCardExpiry)
    private String unifiedSocialCreditCode; // 统一社会信用代码 (个体户/企业必填)

    // 品牌信息
    private Integer hasBrand; // 是否有品牌: 0否 1是
    private String brandAuthorizationLetter; // 品牌授权书URL
    private String trademarkRegistrationCert; // 商标注册证URL

    // 身份类 (Identity - modifications need re-audit)
    private String shopName;
    private String licenseImage; // 营业执照照片
    private String idCardFront; // 法人身份证正面
    private String idCardBack; // 法人身份证反面
    private String businessLicenseExpiry; // 营业执照有效期

    // 联系类 (Contact - directly editable)
    private String contactPerson;
    private String contactPhone;
    private String contactEmail;
    private String mainCategories;

    // 配置类 (Configuration - optional, directly editable)
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

    // 财务信息 (Finance - stored in shop_finance table)
    private String bankName;
    private String bankAccount;
    private String accountHolder;
    private String branchName;

    // 文件详细信息列表（提交时一并保存到 shop_certification_file 表）
    // 包含 fileType / fileUrl / fileName / fileSize / fileFormat
    private List<FileDetail> files;

    @Data
    public static class FileDetail {
        private String fileType;     // business_license, id_card_front, id_card_back, brand_authorization, trademark_cert
        private String fileUrl;
        private String fileName;
        private Long fileSize;       // 字节
        private String fileFormat;   // 扩展名，如 jpg/pdf
    }
}
