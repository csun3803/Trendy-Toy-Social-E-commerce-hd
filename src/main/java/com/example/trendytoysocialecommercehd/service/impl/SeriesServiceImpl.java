package com.example.trendytoysocialecommercehd.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.trendytoysocialecommercehd.dto.SeriesDetailDTO;
import com.example.trendytoysocialecommercehd.entity.Series;
import com.example.trendytoysocialecommercehd.mapper.ProductMapper;
import com.example.trendytoysocialecommercehd.mapper.SeriesMapper;
import com.example.trendytoysocialecommercehd.service.SeriesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class SeriesServiceImpl extends ServiceImpl<SeriesMapper, Series> implements SeriesService {

    @Autowired
    private ProductMapper productMapper;
    @Override
    public Page<Series> getSeriesPage(Integer page, Integer size, String keyword) {
        // 创建Page对象
        Page<Series> pageParam = new Page<>(page, size);

        // 调用Mapper方法，返回IPage
        IPage<Series> iPage = baseMapper.selectSeriesPageWithSales(pageParam, keyword);

        // 将IPage转换为Page
        Page<Series> resultPage = new Page<>();
        resultPage.setRecords(iPage.getRecords());
        resultPage.setCurrent(iPage.getCurrent());
        resultPage.setSize(iPage.getSize());
        resultPage.setTotal(iPage.getTotal());
        resultPage.setPages(iPage.getPages());

        return resultPage;
    }

    @Override
    public Series getSeriesWithSales(String seriesId) {
        return baseMapper.selectSeriesWithSales(seriesId);
    }

    @Override
    public SeriesDetailDTO getSeriesDetail(String seriesId) {
        // 获取系列详情和商品列表
        SeriesDetailDTO seriesDetail = baseMapper.selectSeriesDetail(seriesId);

        // 如果系列不存在，返回null
        if (seriesDetail == null) {
            return null;
        }

        // 如果需要，可以在这里添加其他业务逻辑
        // 比如：获取IP信息、计算库存总量等

        return seriesDetail;
    }

    @Override
    @Transactional
    public boolean updateSeriesMinPrice(String seriesId) {
        // 查询该系列下商品的最小价格
        BigDecimal minPrice = productMapper.selectMinPriceBySeriesId(seriesId);

        if (minPrice != null) {
            Series series = new Series();
            series.setSeriesId(seriesId);
            series.setMinPrice(minPrice);
            return updateById(series);
        }

        return false;
    }
}