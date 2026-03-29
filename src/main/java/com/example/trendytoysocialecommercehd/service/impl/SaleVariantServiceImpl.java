package com.example.trendytoysocialecommercehd.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.trendytoysocialecommercehd.dto.SaleVariantDTO;
import com.example.trendytoysocialecommercehd.entity.SaleVariant;
import com.example.trendytoysocialecommercehd.mapper.ProductMapper;
import com.example.trendytoysocialecommercehd.mapper.SaleVariantMapper;
import com.example.trendytoysocialecommercehd.service.SaleVariantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
        return this.list(wrapper);
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
        dto.setSalesCount(saleVariant.getSalesCount());
        dto.setCreatedAt(saleVariant.getCreatedAt());
        dto.setUpdatedAt(saleVariant.getUpdatedAt());

        if (saleVariant.getVariantId() != null) {
            String productName = productMapper.selectProductNameByProductId(saleVariant.getVariantId());
            dto.setVariantName(productName);
        }

        return dto;
    }
}
