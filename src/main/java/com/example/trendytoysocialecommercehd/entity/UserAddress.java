package com.example.trendytoysocialecommercehd.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("user_address")
public class UserAddress {
    @TableId(value = "address_id", type = IdType.INPUT)
    private String addressId;

    @TableField("user_id")
    private String userId;

    @TableField("recipient_name")
    private String recipientName;

    @TableField("recipient_phone")
    private String recipientPhone;

    private String country;

    private String province;

    private String city;

    private String district;

    private String street;

    @TableField("detail_address")
    private String detailAddress;

    @TableField("full_address")
    private String fullAddress;

    @TableField("postal_code")
    private String postalCode;

    @TableField("address_tag")
    private String addressTag;

    @TableField("is_default")
    private Boolean isDefault;

    private BigDecimal longitude;

    private BigDecimal latitude;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}