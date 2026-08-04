package com.example.trendytoysocialecommercehd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.trendytoysocialecommercehd.entity.ShopFinance;
import com.example.trendytoysocialecommercehd.mapper.ShopFinanceMapper;
import org.springframework.stereotype.Service;

@Service
public class ShopFinanceService extends ServiceImpl<ShopFinanceMapper, ShopFinance> {

    public ShopFinance getByShopId(String shopId) {
        LambdaQueryWrapper<ShopFinance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShopFinance::getShopId, shopId);
        return this.getOne(wrapper);
    }

    public void saveOrUpdateByShopId(String shopId, ShopFinance finance) {
        ShopFinance existing = getByShopId(shopId);
        if (existing != null) {
            finance.setFinanceId(existing.getFinanceId());
            finance.setShopId(shopId);
            this.updateById(finance);
        } else {
            finance.setShopId(shopId);
            this.save(finance);
        }
    }
}
