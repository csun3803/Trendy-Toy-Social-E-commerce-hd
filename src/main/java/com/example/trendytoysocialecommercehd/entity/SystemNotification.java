package com.example.trendytoysocialecommercehd.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

/**
 * 系统通知实体
 */
@Data
@TableName("system_notification")
public class SystemNotification {

    @TableId(value = "notification_id", type = IdType.INPUT)
    private String notificationId;

    /** 用户ID（通知接收者） */
    private String userId;

    /** 通知标题 */
    private String title;

    /** 通知内容摘要 */
    private String content;

    /** 通知分类: COUPON优惠券/ORDER订单/INTERACTION互动/SYSTEM系统 */
    private String category;

    /** 关联的业务ID（如优惠券ID、订单ID等） */
    private String relatedId;

    /** 关联的业务类型（如 user_coupon / order 等），用于跳转 */
    private String relatedType;

    /** 是否已读: false未读/true已读 */
    private Boolean isRead;

    /** 已读时间 */
    private Date readAt;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private Date createdAt;
}
