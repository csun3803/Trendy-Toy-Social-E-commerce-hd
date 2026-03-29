package com.example.trendytoysocialecommercehd.dto;

import lombok.Data;

@Data
public class MerchantRegisterDTO {
    private String username;
    private String password;
    private String confirmPassword;
}