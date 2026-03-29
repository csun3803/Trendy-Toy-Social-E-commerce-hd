package com.example.trendytoysocialecommercehd.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("platform_admin")
public class PlatformAdmin {
    @TableId(type = IdType.ASSIGN_UUID)
    private String adminId;
    private String employeeId;
    private String passwordHash;
    private String adminLevel;
    private String department;
    private String position;
    private String managementScope;
    private String systemPermissions;
    private String dataPermissions;
    private String operationPermissions;
    private String approvalPermissions;
    private String accountStatus;
    private Date activatedAt;
    private Date deactivatedAt;
    private Date lastLoginTime;
    private Date lastOperationTime;
}