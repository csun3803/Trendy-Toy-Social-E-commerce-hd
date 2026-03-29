package com.example.trendytoysocialecommercehd.dto;

import com.example.trendytoysocialecommercehd.entity.SaleVariant;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
public class SaleVariantDTO extends SaleVariant {

    private String variantName;
}
