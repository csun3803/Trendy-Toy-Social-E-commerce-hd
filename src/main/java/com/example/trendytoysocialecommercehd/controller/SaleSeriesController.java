package com.example.trendytoysocialecommercehd.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.trendytoysocialecommercehd.common.Result;
import com.example.trendytoysocialecommercehd.entity.SaleSeries;
import com.example.trendytoysocialecommercehd.service.SaleSeriesService;
import com.example.trendytoysocialecommercehd.service.SaleVariantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/sale-series")
@Tag(name = "销售系列管理", description = "销售系列管理接口")
public class SaleSeriesController {

    @Autowired
    private SaleSeriesService saleSeriesService;

    @Autowired
    private SaleVariantService saleVariantService;

    @GetMapping
    @Operation(summary = "分页查询所有销售系列", description = "获取所有销售系列列表")
    public Result<Page<SaleSeries>> getAllSaleSeries(
            @Parameter(description = "页码", example = "1")
            @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量", example = "10")
            @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "搜索关键词")
            @RequestParam(required = false) String keyword) {
        Page<SaleSeries> saleSeriesPage = saleSeriesService.page(new Page<>(page, size), keyword);
        return Result.success(saleSeriesPage);
    }

    @GetMapping("/shop/{shopId}")
    @Operation(summary = "根据商家ID获取销售系列", description = "获取指定商家的所有销售系列")
    public Result<List<SaleSeries>> getSaleSeriesByShopId(
            @Parameter(description = "商家ID", required = true)
            @PathVariable String shopId) {
        List<SaleSeries> saleSeriesList = saleSeriesService.getSaleSeriesByShopId(shopId);
        return Result.success(saleSeriesList);
    }

    @GetMapping("/{saleSeriesId}")
    @Operation(summary = "获取销售系列详情", description = "根据ID获取销售系列详细信息")
    public Result<SaleSeries> getSaleSeriesById(
            @Parameter(description = "销售系列ID", required = true)
            @PathVariable String saleSeriesId) {
        SaleSeries saleSeries = saleSeriesService.getById(saleSeriesId);
        if (saleSeries == null) {
            return Result.error("销售系列不存在");
        }
        return Result.success(saleSeries);
    }


    @PostMapping
    @Operation(summary = "创建销售系列", description = "创建新的销售系列")
    public Result<SaleSeries> createSaleSeries(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "销售系列数据")
            @RequestBody SaleSeries saleSeries) {
        if (saleSeries.getSaleSeriesId() == null) {
            saleSeries.setSaleSeriesId(UUID.randomUUID().toString());
        }
        boolean success = saleSeriesService.save(saleSeries);
        if (success) {
            return Result.success(saleSeries);
        }
        return Result.error("创建失败");
    }

    @PutMapping("/{saleSeriesId}")
    @Operation(summary = "更新销售系列", description = "更新指定ID的销售系列信息")
    public Result<SaleSeries> updateSaleSeries(
            @Parameter(description = "销售系列ID", required = true)
            @PathVariable String saleSeriesId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "更新数据")
            @RequestBody SaleSeries saleSeries) {
        saleSeries.setSaleSeriesId(saleSeriesId);
        boolean success = saleSeriesService.updateById(saleSeries);
        if (success) {
            return Result.success(saleSeries);
        }
        return Result.error("更新失败");
    }

    @DeleteMapping("/{saleSeriesId}")
    @Operation(summary = "删除销售系列", description = "删除指定ID的销售系列")
    public Result<Void> deleteSaleSeries(
            @Parameter(description = "销售系列ID", required = true)
            @PathVariable String saleSeriesId) {
        boolean success = saleSeriesService.removeById(saleSeriesId);
        if (success) {
            return Result.success();
        }
        return Result.error("删除失败");
    }
}
