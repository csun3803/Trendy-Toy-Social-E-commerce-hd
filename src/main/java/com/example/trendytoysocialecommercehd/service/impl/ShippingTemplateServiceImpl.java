package com.example.trendytoysocialecommercehd.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.trendytoysocialecommercehd.entity.ShippingTemplate;
import com.example.trendytoysocialecommercehd.mapper.ShippingTemplateMapper;
import com.example.trendytoysocialecommercehd.service.ShippingTemplateService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ShippingTemplateServiceImpl implements ShippingTemplateService {

    @Autowired
    private ShippingTemplateMapper shippingTemplateMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<ShippingTemplate> getByShopId(String shopId) {
        LambdaQueryWrapper<ShippingTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShippingTemplate::getShopId, shopId);
        wrapper.orderByDesc(ShippingTemplate::getCreateTime);
        return shippingTemplateMapper.selectList(wrapper);
    }

    @Override
    public ShippingTemplate getById(String templateId) {
        return shippingTemplateMapper.selectById(templateId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ShippingTemplate create(ShippingTemplate template) {
        template.setTemplateId(UUID.randomUUID().toString());
        template.setCreateTime(java.time.LocalDateTime.now());
        template.setUpdateTime(java.time.LocalDateTime.now());
        shippingTemplateMapper.insert(template);
        return template;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ShippingTemplate update(ShippingTemplate template) {
        template.setUpdateTime(java.time.LocalDateTime.now());
        shippingTemplateMapper.updateById(template);
        return template;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String templateId) {
        shippingTemplateMapper.deleteById(templateId);
    }

    @Override
    public BigDecimal calculateShippingFee(String shopId, String province, BigDecimal orderAmount) {
        // 查询店铺的运费模板
        LambdaQueryWrapper<ShippingTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShippingTemplate::getShopId, shopId);
        wrapper.last("LIMIT 1");
        ShippingTemplate template = shippingTemplateMapper.selectOne(wrapper);

        // 无模板则免运费
        if (template == null) {
            return BigDecimal.ZERO;
        }

        // 解析区域规则
        if (template.getRegionalRules() != null && !template.getRegionalRules().isEmpty()) {
            try {
                List<Map<String, Object>> rules = objectMapper.readValue(
                    template.getRegionalRules(),
                    new TypeReference<List<Map<String, Object>>>() {}
                );

                // 遍历区域规则，匹配省份
                for (Map<String, Object> rule : rules) {
                    @SuppressWarnings("unchecked")
                    List<String> regions = (List<String>) rule.get("regions");
                    if (regions != null && matchProvince(province, regions)) {
                        // 命中区域规则
                        BigDecimal ruleFee = toBigDecimal(rule.get("fee"));
                        Object freeThresholdObj = rule.get("freeThreshold");
                        BigDecimal ruleFreeThreshold = freeThresholdObj != null ? toBigDecimal(freeThresholdObj) : null;

                        // 检查该区域的包邮门槛
                        if (ruleFreeThreshold != null && ruleFreeThreshold.compareTo(BigDecimal.ZERO) > 0
                            && orderAmount.compareTo(ruleFreeThreshold) >= 0) {
                            return BigDecimal.ZERO;
                        }
                        return ruleFee != null ? ruleFee : BigDecimal.ZERO;
                    }
                }
            } catch (Exception e) {
                System.err.println("解析区域运费规则失败: " + e.getMessage());
            }
        }

        // 未命中任何区域规则，使用模板默认值
        if (template.getFreeShippingThreshold() != null
            && template.getFreeShippingThreshold().compareTo(BigDecimal.ZERO) > 0
            && orderAmount.compareTo(template.getFreeShippingThreshold()) >= 0) {
            return BigDecimal.ZERO;
        }

        return template.getDefaultFee() != null ? template.getDefaultFee() : BigDecimal.ZERO;
    }

    /**
     * 匹配省份：支持精确匹配和包含匹配（如"新疆"匹配"新疆维吾尔自治区"）
     */
    private boolean matchProvince(String province, List<String> regions) {
        if (province == null || province.isEmpty()) return false;
        for (String region : regions) {
            if (province.equals(region) || province.contains(region) || region.contains(province)) {
                return true;
            }
        }
        return false;
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return null;
        if (value instanceof BigDecimal) return (BigDecimal) value;
        if (value instanceof Number) return BigDecimal.valueOf(((Number) value).doubleValue());
        try {
            return new BigDecimal(value.toString());
        } catch (Exception e) {
            return null;
        }
    }
}
