package com.example.trendytoysocialecommercehd.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("shop_admin")
public class ShopAdmin {
    @TableId(type = IdType.ASSIGN_UUID)
    private String adminId;
    private String shopId;
    private String passwordHash;
    private Integer isActive;
    private Date lastLoginTime;
    private Date lastOperationTime;
    private String auditStatus;
    private String auditNotes;
    private Date auditedAt;
    private Integer loginCount;
}