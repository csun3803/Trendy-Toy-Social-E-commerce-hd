package com.example.trendytoysocialecommercehd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.trendytoysocialecommercehd.entity.Shop;
import com.example.trendytoysocialecommercehd.mapper.ShopMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class ShopService extends ServiceImpl<ShopMapper, Shop> {

    @Autowired
    private ShopMapper shopMapper;

    public Page<Shop> getShopList(int page, int size, String shopStatus, String auditStatus) {
        Page<Shop> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<Shop> wrapper = new LambdaQueryWrapper<>();

        if (shopStatus != null && !shopStatus.isEmpty()) {
            wrapper.eq(Shop::getShopStatus, shopStatus);
        }
        if (auditStatus != null && !auditStatus.isEmpty()) {
            wrapper.eq(Shop::getAuditStatus, auditStatus);
        }
        wrapper.orderByDesc(Shop::getShopId);

        Page<Shop> result = this.page(pageObj, wrapper);
        // 为每个店铺添加销量数据
        for (Shop shop : result.getRecords()) {
            addSalesDataToShop(shop);
        }
        return result;
    }

    public Shop getShopById(String shopId) {
        Shop shop = this.getById(shopId);
        if (shop != null) {
            addSalesDataToShop(shop);
        }
        return shop;
    }

    // 直接给Shop对象添加销量数据
    private void addSalesDataToShop(Shop shop) {
        Integer monthlySales = shopMapper.getMonthlySales(shop.getShopId());
        Integer totalSales = shopMapper.getTotalSales(shop.getShopId());
        BigDecimal totalSalesAmount = shopMapper.getTotalSalesAmount(shop.getShopId());

        shop.setMonthlySales(monthlySales);
        shop.setTotalSales(totalSales);
        shop.setTotalSalesAmount(totalSalesAmount);
    }

    public void createShop(Shop shop) {
        this.save(shop);
    }

    public void updateShop(String shopId, Shop shop) {
        Shop existing = this.getById(shopId);
        if (existing == null) {
            throw new RuntimeException("商家不存在");
        }
        shop.setShopId(shopId);
        this.updateById(shop);
    }

    public void deleteShop(String shopId) {
        this.removeById(shopId);
    }

    public void approveShop(String shopId, String auditorId) {
        Shop shop = this.getById(shopId);
        if (shop == null) {
            throw new RuntimeException("商家不存在");
        }
        shop.setAuditStatus("已通过");
        shop.setShopStatus("正常营业");
        shop.setBusinessStatus("营业中");
        if (auditorId != null) {
            shop.setAuditorId(auditorId);
        }
        shop.setAuditedAt(java.time.LocalDateTime.now());
        this.updateById(shop);
    }

    public void rejectShop(String shopId, String auditNotes, String auditorId) {
        Shop shop = this.getById(shopId);
        if (shop == null) {
            throw new RuntimeException("商家不存在");
        }
        shop.setAuditStatus("已拒绝");
        shop.setAuditNotes(auditNotes);
        if (auditorId != null) {
            shop.setAuditorId(auditorId);
        }
        shop.setAuditedAt(java.time.LocalDateTime.now());
        this.updateById(shop);
    }
}