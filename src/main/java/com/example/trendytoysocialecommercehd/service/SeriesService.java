package com.example.trendytoysocialecommercehd.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.trendytoysocialecommercehd.dto.SeriesDetailDTO;
import com.example.trendytoysocialecommercehd.entity.Series;

public interface SeriesService extends IService<Series> {

    /**
     * 分页查询系列（包含销量）
     */
    Page<Series> getSeriesPage(Integer page, Integer size, String keyword);

    /**
     * 获取系列详情（包含销量）
     */
    Series getSeriesWithSales(String seriesId);

    /**
     * 获取系列详情（包含销量和商品列表）
     */
    SeriesDetailDTO getSeriesDetail(String seriesId);

    /**
     * 更新系列的最小价格（根据商品价格）
     */
    boolean updateSeriesMinPrice(String seriesId);
}