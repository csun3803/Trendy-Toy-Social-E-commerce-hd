package com.example.trendytoysocialecommercehd.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.trendytoysocialecommercehd.common.Result;
import com.example.trendytoysocialecommercehd.dto.SeriesDetailDTO;
import com.example.trendytoysocialecommercehd.entity.Series;
import com.example.trendytoysocialecommercehd.service.SeriesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/series")
@Tag(name = "系列管理", description = "潮流玩具系列管理接口")
public class SeriesController {

    @Autowired
    private SeriesService seriesService;

    @GetMapping
    @Operation(summary = "分页查询系列列表", description = "获取系列列表，支持分页和关键词搜索")
    public Result<Page<Series>> getSeriesList(
            @Parameter(description = "页码", example = "1")
            @RequestParam(defaultValue = "1") Integer page,

            @Parameter(description = "每页数量", example = "12")
            @RequestParam(defaultValue = "12") Integer size,

            @Parameter(description = "搜索关键词")
            @RequestParam(required = false) String keyword) {

        Page<Series> seriesPage = seriesService.getSeriesPage(page, size, keyword);
        return Result.success(seriesPage);
    }

    @GetMapping("/{seriesId}")
    @Operation(summary = "获取系列详情", description = "根据ID获取系列详细信息，包含销量统计和商品列表")
    public Result<SeriesDetailDTO> getSeriesDetail(
            @Parameter(description = "系列ID", required = true, example = "series_001")
            @PathVariable String seriesId) {

        try {
            SeriesDetailDTO seriesDetail = seriesService.getSeriesDetail(seriesId);
            if (seriesDetail == null) {
                return Result.error("系列不存在");
            }
            return Result.success(seriesDetail);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取详情失败: " + e.getMessage());
        }
    }

    @PostMapping
    @Operation(summary = "创建系列", description = "创建新的系列")
    public Result<Series> createSeries(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "系列数据")
            @RequestBody Series series) {

        boolean success = seriesService.save(series);
        if (success) {
            return Result.success(series);
        }
        return Result.error("创建失败");
    }

    @PutMapping("/{seriesId}")
    @Operation(summary = "更新系列", description = "更新指定ID的系列信息")
    public Result<Series> updateSeries(
            @Parameter(description = "系列ID", required = true)
            @PathVariable String seriesId,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "更新数据")
            @RequestBody Series series) {

        series.setSeriesId(seriesId);
        boolean success = seriesService.updateById(series);
        if (success) {
            return Result.success(series);
        }
        return Result.error("更新失败");
    }

    @DeleteMapping("/{seriesId}")
    @Operation(summary = "删除系列", description = "删除指定ID的系列")
    public Result<Void> deleteSeries(
            @Parameter(description = "系列ID", required = true)
            @PathVariable String seriesId) {

        boolean success = seriesService.removeById(seriesId);
        if (success) {
            return Result.success();
        }
        return Result.error("删除失败");
    }
}