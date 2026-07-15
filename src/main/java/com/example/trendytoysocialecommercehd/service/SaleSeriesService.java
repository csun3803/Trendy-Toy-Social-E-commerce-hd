package com.example.trendytoysocialecommercehd.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.trendytoysocialecommercehd.entity.SaleSeries;

import java.util.List;

public interface SaleSeriesService extends IService<SaleSeries> {
    List<SaleSeries> getSaleSeriesWithPriceByShopId(String shopId);
    List<SaleSeries> getAllSaleSeriesWithPrice();
    SaleSeries getSaleSeriesDetailById(String saleSeriesId);

    boolean deleteSaleSeriesWithVariants(String saleSeriesId);
}