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
    private String businessEntityType;
    private String legalPersonName;
    private String legalPersonIdCard;
    private String legalPersonIdCardExpiry;
    private String unifiedSocialCreditCode;
    private String businessLicenseExpiry;
    private String registeredCapital;
    private LocalDate establishmentDate;
    private String businessScope;
    private String registeredAddress;
    private LocalDate authorizationDate;
    private LocalDate authorizationExpiry;
    private String authorizationLevel;
    private String trademarkRegistrationNo;
    private String trademarkInternationalClass;
    private String trademarkOwner;
    private LocalDate trademarkExpiry;
    private String bankName;
    private String bankAccount;
    private String accountHolder;
    private String branchName;
    private String shopIntro;
    private String mainCategories;
    private String mainIps;
    private String customerServicePhone;
    private String customerServiceEmail;
    private Integer authenticityGuarantee;
    private String fakeCompensation;
    private Integer followerCount;
    private Integer productCount;
    private BigDecimal shopRating;
    private BigDecimal refundRate;
    private String shopStatus;
    private String businessStatus;
    private String freeShippingSetting;
    private BigDecimal platformCommissionRate;
    private BigDecimal techServiceRate;
    private BigDecimal depositBalance;
    private String depositStatus;
    private String auditStatus;
    private String auditNotes;
    private LocalDateTime auditedAt;
    private String auditorId;
    private Integer auditRound;

    // 临时字段，不映射到数据库
    @TableField(exist = false)
    private Integer monthlySales;

    @TableField(exist = false)
    private Integer totalSales;

    @TableField(exist = false)
    private BigDecimal totalSalesAmount;
}