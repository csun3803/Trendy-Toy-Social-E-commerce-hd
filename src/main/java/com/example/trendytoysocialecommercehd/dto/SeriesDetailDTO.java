package com.example.trendytoysocialecommercehd.dto;

import com.example.trendytoysocialecommercehd.entity.Product;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;

@Data
public class SeriesDetailDTO {
    // 系列基本信息
    private String seriesId;
    private String seriesName;
    private String ipAlbumId;
    private String theme;
    private Integer releaseYear;
    private String description;
    private String coverImage; // JSON字符串
    private Integer regularVariants;
    private Integer hiddenVariants;
    private Integer totalVariants;
    private Boolean isLimited;
    private Integer limitedQuantity;
    private BigDecimal minPrice;
    private BigDecimal fullsetPrice;
    private String status;
    private Integer seriesHotness;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer salesCount;
    private Date createTime;
    private Date updateTime;

    // 商品列表
    private List<ProductDetailDTO> products;

    // IP信息（可选）
    private String ipName;

    @Data
    public static class ProductDetailDTO {
        private String productId;
        private String productName;
        private String variantType; // regular/hidden
        private String rarity; // common/rare/secret
        private BigDecimal price;
        private Integer stock;
        private String description;
        private String productImages; // JSON字符串
        private Integer sortOrder;
        private String status;

        // 额外信息
        private String category;
        private String brand;
        private String designer;
        private String material;
        private String dimensions;
        private LocalDate releaseDate;
        private Integer marketHotness;
    }
}