package com.example.trendytoysocialecommercehd.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.trendytoysocialecommercehd.entity.SaleVariant;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface SaleVariantMapper extends BaseMapper<SaleVariant> {

    @Delete("DELETE FROM sale_variant WHERE sale_series_id = #{saleSeriesId} COLLATE utf8mb4_unicode_ci")
    int deleteBySaleSeriesId(String saleSeriesId);

    @Select("SELECT COALESCE(SUM(oi.quantity), 0) " +
            "FROM order_items oi " +
            "JOIN orders o ON oi.order_id = o.order_id COLLATE utf8mb4_unicode_ci " +
            "WHERE oi.product_id = #{saleVariantId} COLLATE utf8mb4_unicode_ci " +
            "AND o.order_status NOT IN ('CANCELLED', 'REFUNDED')")
    int selectSalesCountByVariantId(@Param("saleVariantId") String saleVariantId);

    /**
     * 按系列名称模糊查询销售款式列表（含款式名、价格、库存），供AI工具接口使用
     * 匹配 sale_series.sale_title 或 series.series_name
     */
    @Select("SELECT sv.sale_variant_id AS saleVariantId, sv.sale_series_id AS saleSeriesId, " +
            "sv.sale_price AS salePrice, sv.crossed_price AS crossedPrice, " +
            "sv.stock_quantity AS stockQuantity, sv.sale_status AS saleStatus, " +
            "sv.sku_code AS skuCode, sv.custom_description AS customDescription, " +
            "p.name AS variantName, p.is_hidden_variant AS isHidden, " +
            "ss.sale_title AS saleTitle, s.series_name AS seriesName " +
            "FROM sale_variant sv " +
            "LEFT JOIN sale_series ss ON sv.sale_series_id = ss.sale_series_id COLLATE utf8mb4_unicode_ci " +
            "LEFT JOIN series s ON ss.series_id = s.series_id COLLATE utf8mb4_unicode_ci " +
            "LEFT JOIN product p ON sv.variant_id = p.product_id COLLATE utf8mb4_unicode_ci " +
            "WHERE (ss.sale_title LIKE CONCAT('%', #{seriesName}, '%') " +
            "       OR s.series_name LIKE CONCAT('%', #{seriesName}, '%')) " +
            "ORDER BY ss.sale_series_id, p.is_hidden_variant ASC, p.series_order ASC")
    List<Map<String, Object>> selectStylesBySeriesName(@Param("seriesName") String seriesName);

    /**
     * 按款式名称模糊查询库存信息，供AI工具接口使用
     * 匹配 product.name 或 sale_variant.custom_description / sku_code
     */
    @Select("SELECT sv.sale_variant_id AS saleVariantId, " +
            "sv.sale_price AS salePrice, sv.stock_quantity AS stockQuantity, " +
            "sv.sale_status AS saleStatus, sv.sku_code AS skuCode, " +
            "sv.custom_description AS customDescription, " +
            "p.name AS variantName, p.stock AS productStock, p.status AS productStatus, " +
            "p.is_hidden_variant AS isHidden, " +
            "ss.sale_title AS saleTitle, s.series_name AS seriesName " +
            "FROM sale_variant sv " +
            "LEFT JOIN product p ON sv.variant_id = p.product_id COLLATE utf8mb4_unicode_ci " +
            "LEFT JOIN sale_series ss ON sv.sale_series_id = ss.sale_series_id COLLATE utf8mb4_unicode_ci " +
            "LEFT JOIN series s ON ss.series_id = s.series_id COLLATE utf8mb4_unicode_ci " +
            "WHERE p.name LIKE CONCAT('%', #{styleName}, '%') " +
            "   OR sv.custom_description LIKE CONCAT('%', #{styleName}, '%') " +
            "   OR sv.sku_code LIKE CONCAT('%', #{styleName}, '%') " +
            "ORDER BY sv.stock_quantity DESC " +
            "LIMIT 20")
    List<Map<String, Object>> selectStyleStockByName(@Param("styleName") String styleName);
}
