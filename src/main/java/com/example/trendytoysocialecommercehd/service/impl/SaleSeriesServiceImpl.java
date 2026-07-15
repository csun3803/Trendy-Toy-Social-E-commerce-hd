package com.example.trendytoysocialecommercehd.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.trendytoysocialecommercehd.entity.SaleSeries;
import com.example.trendytoysocialecommercehd.mapper.SaleSeriesMapper;
import com.example.trendytoysocialecommercehd.service.SaleSeriesService;
import com.example.trendytoysocialecommercehd.service.SaleVariantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SaleSeriesServiceImpl extends ServiceImpl<SaleSeriesMapper, SaleSeries> implements SaleSeriesService {

    @Autowired
    private SaleVariantService saleVariantService;

    @Override
    public List<SaleSeries> getSaleSeriesWithPriceByShopId(String shopId) {
        return baseMapper.selectSaleSeriesWithPriceByShopId(shopId);
    }

    @Override
    public List<SaleSeries> getAllSaleSeriesWithPrice() {
        return baseMapper.selectAllSaleSeriesWithPrice();
    }

    @Override
    public SaleSeries getSaleSeriesDetailById(String saleSeriesId) {
        return baseMapper.selectSaleSeriesDetailById(saleSeriesId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteSaleSeriesWithVariants(String saleSeriesId) {
        // 先删除该销售系列下的所有销售款式
        saleVariantService.deleteBySaleSeriesId(saleSeriesId);
        // 再删除销售系列本身
        return this.removeById(saleSeriesId);
    }
}