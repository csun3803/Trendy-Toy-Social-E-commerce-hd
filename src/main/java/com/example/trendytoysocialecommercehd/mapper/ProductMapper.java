package com.example.trendytoysocialecommercehd.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.trendytoysocialecommercehd.dto.SeriesDetailDTO;
import com.example.trendytoysocialecommercehd.entity.Product;
import com.example.trendytoysocialecommercehd.dto.SeriesDetailDTO.ProductDetailDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    /**
     * 根据系列ID查询商品列表
     */
    @Select("SELECT p.*, " +
            "CASE WHEN p.is_hidden_variant = 1 THEN 'hidden' ELSE 'regular' END as variant_type, " +
            "CASE WHEN p.is_hidden_variant = 1 THEN 'secret' " +
            "     WHEN p.series_order = 1 THEN 'rare' " +
            "     ELSE 'common' END as rarity " +
            "FROM product p " +
            "WHERE p.series_id = #{seriesId} " +
            "ORDER BY p.is_hidden_variant, p.series_order")
    @Results({
            @Result(property = "productId", column = "product_id"),
            @Result(property = "productName", column = "name"),
            @Result(property = "price", column = "price"),
            @Result(property = "stock", column = "stock"),
            @Result(property = "description", column = "description"),
            @Result(property = "productImages", column = "image_url"),
            @Result(property = "sortOrder", column = "series_order"),
            @Result(property = "status", column = "status"),
            @Result(property = "variantType", column = "variant_type"),
            @Result(property = "rarity", column = "rarity"),
            @Result(property = "category", column = "category"),
            @Result(property = "brand", column = "brand"),
            @Result(property = "designer", column = "designer"),
            @Result(property = "material", column = "material"),
            @Result(property = "dimensions", column = "dimensions"),
            @Result(property = "releaseDate", column = "release_date"),
            @Result(property = "marketHotness", column = "market_hotness")
    })
    List<SeriesDetailDTO.ProductDetailDTO> selectProductsBySeriesId(@Param("seriesId") String seriesId);

    /**
     * 查询系列下商品的最小价格
     */
    @Select("SELECT MIN(price) FROM product WHERE series_id = #{seriesId}")
    BigDecimal selectMinPriceBySeriesId(@Param("seriesId") String seriesId);

    /**
     * 根据产品 ID 查询产品名称
     */
    @Select("SELECT name FROM product WHERE product_id = #{productId}")
    String selectProductNameByProductId(@Param("productId") String productId);

}