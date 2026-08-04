package com.example.trendytoysocialecommercehd.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.trendytoysocialecommercehd.dto.SaleVariantDTO;
import com.example.trendytoysocialecommercehd.entity.SaleVariant;

import java.util.List;

public interface SaleVariantService extends IService<SaleVariant> {
    List<SaleVariant> getSaleVariantsBySaleSeriesId(String saleSeriesId);

    List<SaleVariantDTO> getSaleVariantsWithNamesBySaleSeriesId(String saleSeriesId);

    SaleVariantDTO getSaleVariantWithName(String saleVariantId);

    int deleteBySaleSeriesId(String saleSeriesId);

    boolean batchUpdateStatus(List<String> ids, String saleStatus);

    Page<SaleVariantDTO> searchVariantsWithNames(String keyword, int page, int size);
}

