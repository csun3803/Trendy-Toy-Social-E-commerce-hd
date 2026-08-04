package com.example.trendytoysocialecommercehd.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("shop_config")
public class ShopConfig {
    @TableId(value = "config_id", type = IdType.AUTO)
    private Long configId;

    private String shopId;
    private BigDecimal platformCommissionRate;
    private BigDecimal techServiceRate;
    private String freeShippingSetting;
    private Integer authenticityGuarantee;
    private String fakeCompensation;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
