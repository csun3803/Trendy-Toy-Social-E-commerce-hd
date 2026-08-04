package com.example.trendytoysocialecommercehd.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.trendytoysocialecommercehd.dto.SaleVariantDTO;
import com.example.trendytoysocialecommercehd.entity.SaleVariant;
import com.example.trendytoysocialecommercehd.mapper.ProductMapper;
import com.example.trendytoysocialecommercehd.mapper.SaleVariantMapper;
import com.example.trendytoysocialecommercehd.service.SaleVariantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SaleVariantServiceImpl extends ServiceImpl<SaleVariantMapper, SaleVariant> implements SaleVariantService {

    @Autowired
    private ProductMapper productMapper;

    @Override
    public List<SaleVariant> getSaleVariantsBySaleSeriesId(String saleSeriesId) {
        LambdaQueryWrapper<SaleVariant> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SaleVariant::getSaleSeriesId, saleSeriesId);
        wrapper.orderByDesc(SaleVariant::getCreatedAt);
        List<SaleVariant> list = this.list(wrapper);
        for (SaleVariant variant : list) {
            variant.setSalesCount(baseMapper.selectSalesCountByVariantId(variant.getSaleVariantId()));
        }
        return list;
    }

    @Override
    public List<SaleVariantDTO> getSaleVariantsWithNamesBySaleSeriesId(String saleSeriesId) {
        LambdaQueryWrapper<SaleVariant> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SaleVariant::getSaleSeriesId, saleSeriesId);
        wrapper.orderByDesc(SaleVariant::getCreatedAt);
        List<SaleVariant> saleVariants = this.list(wrapper);

        return saleVariants.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public SaleVariantDTO getSaleVariantWithName(String saleVariantId) {
        SaleVariant saleVariant = this.getById(saleVariantId);
        if (saleVariant == null) {
            return null;
        }
        return convertToDTO(saleVariant);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteBySaleSeriesId(String saleSeriesId) {
        return baseMapper.deleteBySaleSeriesId(saleSeriesId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchUpdateStatus(List<String> ids, String saleStatus) {
        for (String id : ids) {
            SaleVariant variant = new SaleVariant();
            variant.setSaleVariantId(id);
            variant.setSaleStatus(saleStatus);
            if (!this.updateById(variant)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Page<SaleVariantDTO> searchVariantsWithNames(String keyword, int page, int size) {
        // 先通过product表按关键字搜索，获取匹配的variantId列表
        List<String> matchedVariantIds = productMapper.searchProductIdsByKeyword(keyword);

        Page<SaleVariant> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<SaleVariant> wrapper = new LambdaQueryWrapper<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            if (matchedVariantIds.isEmpty()) {
                // 没有匹配结果，返回空页
                return new Page<>(page, size);
            }
            wrapper.in(SaleVariant::getVariantId, matchedVariantIds);
        }
        wrapper.eq(SaleVariant::getSaleStatus, "ON_SALE");
        wrapper.orderByDesc(SaleVariant::getCreatedAt);

        Page<SaleVariant> result = this.page(pageObj, wrapper);
        Page<SaleVariantDTO> dtoPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        dtoPage.setRecords(result.getRecords().stream().map(this::convertToDTO).collect(Collectors.toList()));
        return dtoPage;
    }

    private SaleVariantDTO convertToDTO(SaleVariant saleVariant) {
        SaleVariantDTO dto = new SaleVariantDTO();
        dto.setSaleVariantId(saleVariant.getSaleVariantId());
        dto.setSaleSeriesId(saleVariant.getSaleSeriesId());
        dto.setVariantId(saleVariant.getVariantId());
        dto.setShopId(saleVariant.getShopId());
        dto.setSalePrice(saleVariant.getSalePrice());
        dto.setCrossedPrice(saleVariant.getCrossedPrice());
        dto.setStockQuantity(saleVariant.getStockQuantity());
        dto.setWarningStock(saleVariant.getWarningStock());
        dto.setSkuCode(saleVariant.getSkuCode());
        dto.setSaleStatus(saleVariant.getSaleStatus());
        dto.setLimitQuantity(saleVariant.getLimitQuantity());
        dto.setCustomDescription(saleVariant.getCustomDescription());
        dto.setCustomImages(saleVariant.getCustomImages());
        dto.setSalesCount(baseMapper.selectSalesCountByVariantId(saleVariant.getSaleVariantId()));
        dto.setCreatedAt(saleVariant.getCreatedAt());
        dto.setUpdatedAt(saleVariant.getUpdatedAt());

        if (saleVariant.getVariantId() != null) {
            String productName = productMapper.selectProductNameByProductId(saleVariant.getVariantId());
            dto.setVariantName(productName);
        }

        return dto;
    }
}