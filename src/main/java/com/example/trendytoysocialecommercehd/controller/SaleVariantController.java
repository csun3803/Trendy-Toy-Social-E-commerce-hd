package com.example.trendytoysocialecommercehd.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.trendytoysocialecommercehd.common.Result;
import com.example.trendytoysocialecommercehd.dto.SaleVariantDTO;
import com.example.trendytoysocialecommercehd.entity.SaleVariant;
import com.example.trendytoysocialecommercehd.service.SaleVariantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/sale-variants")
@Tag(name = "销售款式管理", description = "销售款式管理接口")
public class SaleVariantController {

    @Autowired
    private SaleVariantService saleVariantService;

    @GetMapping
    @Operation(summary = "分页查询所有销售款式", description = "获取所有销售款式列表")
    public Result<Page<SaleVariant>> getAllSaleVariants(
            @Parameter(description = "页码", example = "1")
            @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量", example = "10")
            @RequestParam(defaultValue = "10") Integer size) {
        Page<SaleVariant> saleVariantPage = saleVariantService.page(new Page<>(page, size));
        return Result.success(saleVariantPage);
    }

    @GetMapping("/sale-series/{saleSeriesId}")
    @Operation(summary = "根据销售系列ID获取款式", description = "获取指定销售系列的所有款式")
    public Result<List<SaleVariant>> getSaleVariantsBySaleSeriesId(
            @Parameter(description = "销售系列ID", required = true)
            @PathVariable String saleSeriesId) {
        List<SaleVariant> saleVariants = saleVariantService.getSaleVariantsBySaleSeriesId(saleSeriesId);
        return Result.success(saleVariants);
    }

    @GetMapping("/sale-series/{saleSeriesId}/with-names")
    @Operation(summary = "根据销售系列 ID 获取款式 (含款式名)", description = "获取指定销售系列的所有款式，包含款式名称")
    public Result<List<SaleVariantDTO>> getSaleVariantsWithNamesBySaleSeriesId(
            @Parameter(description = "销售系列 ID", required = true)
            @PathVariable String saleSeriesId) {
        List<SaleVariantDTO> saleVariants = saleVariantService.getSaleVariantsWithNamesBySaleSeriesId(saleSeriesId);
        return Result.success(saleVariants);
    }

    @GetMapping("/{saleVariantId}")
    @Operation(summary = "获取销售款式详情", description = "根据ID获取销售款式详细信息")
    public Result<SaleVariant> getSaleVariantById(
            @Parameter(description = "销售款式ID", required = true)
            @PathVariable String saleVariantId) {
        SaleVariant saleVariant = saleVariantService.getById(saleVariantId);
        if (saleVariant == null) {
            return Result.error("销售款式不存在");
        }
        return Result.success(saleVariant);
    }

    @PostMapping
    @Operation(summary = "创建销售款式", description = "创建新的销售款式")
    public Result<SaleVariant> createSaleVariant(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "销售款式数据")
            @RequestBody SaleVariant saleVariant) {
        if (saleVariant.getSaleVariantId() == null) {
            saleVariant.setSaleVariantId(UUID.randomUUID().toString());
        }
        boolean success = saleVariantService.save(saleVariant);
        if (success) {
            return Result.success(saleVariant);
        }
        return Result.error("创建失败");
    }

    @PutMapping("/{saleVariantId}")
    @Operation(summary = "更新销售款式", description = "更新指定ID的销售款式信息")
    public Result<SaleVariant> updateSaleVariant(
            @Parameter(description = "销售款式ID", required = true)
            @PathVariable String saleVariantId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "更新数据")
            @RequestBody SaleVariant saleVariant) {
        saleVariant.setSaleVariantId(saleVariantId);
        boolean success = saleVariantService.updateById(saleVariant);
        if (success) {
            return Result.success(saleVariant);
        }
        return Result.error("更新失败");
    }

    @DeleteMapping("/{saleVariantId}")
    @Operation(summary = "删除销售款式", description = "删除指定ID的销售款式")
    public Result<Void> deleteSaleVariant(
            @Parameter(description = "销售款式ID", required = true)
            @PathVariable String saleVariantId) {
        boolean success = saleVariantService.removeById(saleVariantId);
        if (success) {
            return Result.success();
        }
        return Result.error("删除失败");
    }
}
