package com.example.trendytoysocialecommercehd.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("shop")
public class Shop {
    @TableId(value = "shop_id", type = IdType.INPUT)
    private String shopId;

    private String shopName;
    private String shopCover;

    @TableField(exist = false)
    private String shopLogo;

    private String shopType;
    private String legalPersonName;
    private String unifiedSocialCreditCode;
    private String businessLicenseExpiry;
    private String registeredCapital;
    private LocalDate establishmentDate;
    private String businessScope;
    private String registeredAddress;
    private String shopIntro;
    private String mainCategories;
    private String mainIps;
    private String customerServicePhone;
    private String customerServiceEmail;
    private Integer followerCount;
    private Integer productCount;
    private BigDecimal shopRating;
    private BigDecimal refundRate;
    private String shopStatus;
    private String businessStatus;
    private String auditStatus;
    private String auditNotes;
    private LocalDateTime auditedAt;
    private String auditorId;
    private Integer auditRound;

    // 新增字段
    private String userId;
    private String contactPerson;
    private String contactPhone;
    private String contactEmail;
    private String legalPersonIdCardNumber; // 法人身份证号码 (原 idCardNumber)
    private String legalPersonIdCardExpiry; // 法人身份证有效期 (原 idCardExpiry)
    private String returnAddressProvince;
    private String returnAddressCity;
    private String returnAddressDistrict;
    private String returnAddressDetail;
    private String returnAddressContact;
    private String returnAddressPhone;

    private Integer subjectType; // 主体类型: 0个人 1个体户 2企业
    private String applySn;      // 申请单号
    private String pendingData;  // 待审核的身份字段变更JSON

    // 品牌信息
    private Integer hasBrand; // 是否有品牌: 0否 1是
    private String brandAuthorizationLetter; // 品牌授权书文件ID
    private String trademarkRegistrationCert; // 商标注册证文件ID

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 临时字段，不映射到数据库 - 文件URL从 shop_certification_file 表查询填充
    @TableField(exist = false)
    private String licenseImage; // 营业执照照片URL

    @TableField(exist = false)
    private String idCardFront;  // 身份证正面照片URL

    @TableField(exist = false)
    private String idCardBack;   // 身份证背面照片URL

    // 临时字段 - 财务信息从 shop_finance 表查询填充
    @TableField(exist = false)
    private String bankName;          // 开户行

    @TableField(exist = false)
    private String bankAccount;       // 银行账号

    @TableField(exist = false)
    private String accountHolder;     // 账户持有人

    @TableField(exist = false)
    private String branchName;        // 支行名称

    @TableField(exist = false)
    private Integer monthlySales;

    @TableField(exist = false)
    private Integer totalSales;

    @TableField(exist = false)
    private BigDecimal totalSalesAmount;

    @TableField(exist = false)
    private String auditorName; // 审核员名称（从 platform_admin 表关联填充）
}
