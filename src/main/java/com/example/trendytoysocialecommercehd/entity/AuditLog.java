package com.example.trendytoysocialecommercehd.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("audit_log")
public class AuditLog {
    @TableId(type = IdType.ASSIGN_UUID)
    private String logId;

    /** 操作人ID */
    private String operatorId;

    /** 操作人用户名/工号 */
    private String operatorName;

    /** 操作人类型: PLATFORM_ADMIN / SHOP_ADMIN */
    private String operatorType;

    /** 操作类型: CREATE / UPDATE / DELETE / LOGIN / APPROVE / REJECT 等 */
    private String action;

    /** 操作模块: USER / SHOP / ORDER / ADMIN / ACTIVITY / ALBUM 等 */
    private String module;

    /** 操作描述 */
    private String description;

    /** 操作目标ID */
    private String targetId;

    /** 操作目标类型 */
    private String targetType;

    /** 请求方法: GET/POST/PUT/DELETE */
    private String method;

    /** 请求路径 */
    private String requestUrl;

    /** 请求参数 */
    private String requestParams;

    /** 响应状态码 */
    private Integer responseCode;

    /** 操作者IP */
    private String ipAddress;

    /** 操作时间 */
    private Date createdAt;
}
