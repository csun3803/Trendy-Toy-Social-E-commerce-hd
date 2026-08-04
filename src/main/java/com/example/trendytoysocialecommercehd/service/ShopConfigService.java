package com.example.trendytoysocialecommercehd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.trendytoysocialecommercehd.entity.ShopConfig;
import com.example.trendytoysocialecommercehd.mapper.ShopConfigMapper;
import org.springframework.stereotype.Service;

@Service
public class ShopConfigService extends ServiceImpl<ShopConfigMapper, ShopConfig> {

    public ShopConfig getByShopId(String shopId) {
        LambdaQueryWrapper<ShopConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShopConfig::getShopId, shopId);
        return this.getOne(wrapper);
    }

    public void saveOrUpdateByShopId(String shopId, ShopConfig config) {
        ShopConfig existing = getByShopId(shopId);
        if (existing != null) {
            config.setConfigId(existing.getConfigId());
            config.setShopId(shopId);
            this.updateById(config);
        } else {
            config.setShopId(shopId);
            this.save(config);
        }
    }
}
