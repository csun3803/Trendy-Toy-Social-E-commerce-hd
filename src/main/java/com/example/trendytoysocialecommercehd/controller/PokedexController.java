package com.example.trendytoysocialecommercehd.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.trendytoysocialecommercehd.common.Result;
import com.example.trendytoysocialecommercehd.entity.SaleSeries;
import com.example.trendytoysocialecommercehd.entity.SaleVariant;
import com.example.trendytoysocialecommercehd.entity.Series;
import com.example.trendytoysocialecommercehd.mapper.SaleSeriesMapper;
import com.example.trendytoysocialecommercehd.mapper.SaleVariantMapper;
import com.example.trendytoysocialecommercehd.mapper.SeriesMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 图鉴 Controller - 供App端图鉴功能使用
 * 层级：IP → 系列 → 款式 → 款式详情
 */
@RestController
@RequestMapping("/api/pokedex")
@Tag(name = "图鉴", description = "图鉴功能接口：IP→系列→款式→款式详情")
public class PokedexController {

    @Autowired
    private SeriesMapper seriesMapper;

    @Autowired
    private SaleSeriesMapper saleSeriesMapper;

    @Autowired
    private SaleVariantMapper saleVariantMapper;

    /**
     * 根据系列ID获取所有款式（跨销售系列聚合）
     * Series → SaleSeries(s) → SaleVariant(s)
     */
    @GetMapping("/series/{seriesId}/variants")
    @Operation(summary = "获取系列下所有款式", description = "根据系列ID获取所有款式，聚合多个销售系列")
    public Result<List<Map<String, Object>>> getVariantsBySeriesId(@PathVariable String seriesId) {
        // 1. 查找该系列关联的所有销售系列
        QueryWrapper<SaleSeries> ssWrapper = new QueryWrapper<>();
        ssWrapper.eq("series_id", seriesId);
        List<SaleSeries> saleSeriesList = saleSeriesMapper.selectList(ssWrapper);

        if (saleSeriesList.isEmpty()) {
            return Result.success(new ArrayList<>());
        }

        // 2. 收集所有 saleSeriesId
        List<String> saleSeriesIds = new ArrayList<>();
        Map<String, SaleSeries> ssMap = new HashMap<>();
        for (SaleSeries ss : saleSeriesList) {
            saleSeriesIds.add(ss.getSaleSeriesId());
            ssMap.put(ss.getSaleSeriesId(), ss);
        }

        // 3. 查询所有款式
        QueryWrapper<SaleVariant> svWrapper = new QueryWrapper<>();
        svWrapper.in("sale_series_id", saleSeriesIds);
        svWrapper.orderByAsc("custom_description");
        List<SaleVariant> variants = saleVariantMapper.selectList(svWrapper);

        // 4. 构建返回数据
        List<Map<String, Object>> result = new ArrayList<>();
        for (SaleVariant v : variants) {
            Map<String, Object> map = new HashMap<>();
            map.put("saleVariantId", v.getSaleVariantId());
            map.put("saleSeriesId", v.getSaleSeriesId());
            map.put("variantName", v.getCustomDescription() != null ? v.getCustomDescription() : v.getSkuCode());
            map.put("skuCode", v.getSkuCode());
            map.put("price", v.getSalePrice());
            map.put("crossedPrice", v.getCrossedPrice());
            map.put("images", parseImages(v.getCustomImages()));
            map.put("stockQuantity", v.getStockQuantity());
            map.put("salesCount", v.getSalesCount());
            map.put("saleStatus", v.getSaleStatus());

            // 附加销售系列信息
            SaleSeries ss = ssMap.get(v.getSaleSeriesId());
            if (ss != null) {
                map.put("saleTitle", ss.getSaleTitle());
                map.put("shopId", ss.getShopId());
            }
            result.add(map);
        }

        return Result.success(result);
    }

    /**
     * 获取款式详情
     */
    @GetMapping("/variants/{variantId}")
    @Operation(summary = "获取款式详情", description = "根据款式ID获取详细信息")
    public Result<Map<String, Object>> getVariantDetail(@PathVariable String variantId) {
        SaleVariant variant = saleVariantMapper.selectById(variantId);
        if (variant == null) {
            return Result.error("款式不存在");
        }

        Map<String, Object> map = new HashMap<>();
        map.put("saleVariantId", variant.getSaleVariantId());
        map.put("saleSeriesId", variant.getSaleSeriesId());
        map.put("variantName", variant.getCustomDescription() != null ? variant.getCustomDescription() : variant.getSkuCode());
        map.put("skuCode", variant.getSkuCode());
        map.put("price", variant.getSalePrice());
        map.put("crossedPrice", variant.getCrossedPrice());
        map.put("images", parseImages(variant.getCustomImages()));
        map.put("stockQuantity", variant.getStockQuantity());
        map.put("salesCount", variant.getSalesCount());
        map.put("saleStatus", variant.getSaleStatus());
        map.put("limitQuantity", variant.getLimitQuantity());

        // 附加销售系列和系列信息
        if (variant.getSaleSeriesId() != null) {
            SaleSeries ss = saleSeriesMapper.selectById(variant.getSaleSeriesId());
            if (ss != null) {
                map.put("saleTitle", ss.getSaleTitle());
                map.put("saleCoverImage", ss.getSaleCoverImage());
                map.put("shopId", ss.getShopId());
                map.put("saleDescription", ss.getSaleDescription());

                if (ss.getSeriesId() != null) {
                    Series series = seriesMapper.selectById(ss.getSeriesId());
                    if (series != null) {
                        map.put("seriesId", series.getSeriesId());
                        map.put("seriesName", series.getSeriesName());
                        map.put("theme", series.getTheme());
                        map.put("description", series.getDescription());
                        map.put("coverImage", series.getCoverImage());
                        map.put("releaseYear", series.getReleaseYear());
                        map.put("isLimited", series.getIsLimited());
                    }
                }
            }
        }

        return Result.success(map);
    }

    /** 解析款式图片（返回数组） */
    private List<String> parseImages(String customImages) {
        if (customImages == null || customImages.isEmpty()) return new ArrayList<>();
        try {
            if (customImages.startsWith("[") && customImages.endsWith("]")) {
                String parsed = customImages.substring(1, customImages.length() - 1);
                String[] arr = parsed.split(",");
                List<String> images = new ArrayList<>();
                for (String s : arr) {
                    String img = s.trim().replaceAll("\"", "");
                    if (!img.isEmpty()) images.add(img);
                }
                return images;
            }
            // 逗号分隔
            String[] arr = customImages.split(",");
            List<String> images = new ArrayList<>();
            for (String s : arr) {
                String img = s.trim();
                if (!img.isEmpty()) images.add(img);
            }
            return images;
        } catch (Exception e) {
            return Collections.singletonList(customImages);
        }
    }
}