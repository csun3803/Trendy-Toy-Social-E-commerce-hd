package com.example.trendytoysocialecommercehd.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("merchant_application")
public class MerchantApplication {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String applySn;
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
    private Integer status; // 0待审核 1通过 2驳回
    private String auditRemark;
    private LocalDateTime applyTime;
    private LocalDateTime auditTime;
}
