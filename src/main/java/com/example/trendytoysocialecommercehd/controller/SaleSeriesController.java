package com.example.trendytoysocialecommercehd.controller;

import com.example.trendytoysocialecommercehd.common.Result;
import com.example.trendytoysocialecommercehd.entity.SaleSeries;
import com.example.trendytoysocialecommercehd.service.SaleSeriesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/sale-series")
public class SaleSeriesController {

    @Autowired
    private SaleSeriesService saleSeriesService;

    @GetMapping
    public Result<List<SaleSeries>> getAllSaleSeries() {
        List<SaleSeries> list = saleSeriesService.getAllSaleSeriesWithPrice();
        return Result.success(list);
    }

    @GetMapping("/{saleSeriesId}")
    public Result<SaleSeries> getSaleSeriesById(@PathVariable String saleSeriesId) {
        SaleSeries saleSeries = saleSeriesService.getSaleSeriesDetailById(saleSeriesId);
        return Result.success(saleSeries);
    }

    @GetMapping("/shop/{shopId}")
    public Result<List<SaleSeries>> getSaleSeriesByShopId(@PathVariable String shopId) {
        List<SaleSeries> list = saleSeriesService.getSaleSeriesWithPriceByShopId(shopId);
        return Result.success(list);
    }

    @PostMapping
    public Result<SaleSeries> createSaleSeries(@RequestBody SaleSeries saleSeries) {
        if (saleSeries.getSaleSeriesId() == null || saleSeries.getSaleSeriesId().isEmpty()) {
            saleSeries.setSaleSeriesId(UUID.randomUUID().toString());
        }
        saleSeriesService.save(saleSeries);
        return Result.success(saleSeries);
    }

    @PutMapping("/{saleSeriesId}")
    public Result<SaleSeries> updateSaleSeries(@PathVariable String saleSeriesId, @RequestBody SaleSeries saleSeries) {
        saleSeries.setSaleSeriesId(saleSeriesId);
        saleSeriesService.updateById(saleSeries);
        return Result.success(saleSeries);
    }

    @DeleteMapping("/{saleSeriesId}")
    public Result<Void> deleteSaleSeries(@PathVariable String saleSeriesId) {
        boolean success = saleSeriesService.deleteSaleSeriesWithVariants(saleSeriesId);
        if (success) {
            return Result.success();
        } else {
            return Result.error("删除失败");
        }
    }
}