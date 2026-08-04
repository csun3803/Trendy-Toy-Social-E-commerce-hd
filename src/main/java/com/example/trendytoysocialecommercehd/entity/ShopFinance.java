package com.example.trendytoysocialecommercehd.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("shop_finance")
public class ShopFinance {
    @TableId(value = "finance_id", type = IdType.AUTO)
    private Long financeId;

    private String shopId;
    private String bankName;
    private String bankAccount;
    private String accountHolder;
    private String branchName;
    private BigDecimal depositBalance;
    private String depositStatus;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
