package com.example.trendytoysocialecommercehd.service;

import com.example.trendytoysocialecommercehd.entity.ShippingTemplate;

import java.math.BigDecimal;
import java.util.List;

public interface ShippingTemplateService {
    List<ShippingTemplate> getByShopId(String shopId);
    ShippingTemplate getById(String templateId);
    ShippingTemplate create(ShippingTemplate template);
    ShippingTemplate update(ShippingTemplate template);
    void delete(String templateId);

    /**
     * 计算运费
     * @param shopId 店铺ID
     * @param province 省份名称
     * @param orderAmount 订单金额（不含运费）
     * @return 运费金额
     */
    BigDecimal calculateShippingFee(String shopId, String province, BigDecimal orderAmount);
}
