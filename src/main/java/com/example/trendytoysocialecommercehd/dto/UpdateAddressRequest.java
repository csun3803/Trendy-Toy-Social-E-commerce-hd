package com.example.trendytoysocialecommercehd.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateAddressRequest {
    private String addressId;
    private String recipientName;
    private String recipientPhone;
    private String country;
    private String province;
    private String city;
    private String district;
    private String street;
    private String detailAddress;
    private String postalCode;
    private String addressTag;
    private Boolean isDefault;
    private BigDecimal longitude;
    private BigDecimal latitude;
}