package com.example.trendytoysocialecommercehd.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("USER_COUPON")
public class UserCoupon {
    @TableId(value = "user_coupon_id", type = IdType.INPUT)
    private String userCouponId;

    @TableField("user_id")
    private String userId;

    @TableField("template_id")
    private String templateId;

    @TableField("coupon_code")
    private String couponCode;

    private String status;

    @TableField("claimed_at")
    private LocalDateTime claimedAt;

    @TableField("used_at")
    private LocalDateTime usedAt;

    @TableField("expires_at")
    private LocalDate expiresAt;

    @TableField("order_id")
    private String orderId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
