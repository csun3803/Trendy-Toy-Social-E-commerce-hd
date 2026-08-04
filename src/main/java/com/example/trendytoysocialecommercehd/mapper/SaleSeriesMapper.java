package com.example.trendytoysocialecommercehd.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.trendytoysocialecommercehd.entity.SaleSeries;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SaleSeriesMapper extends BaseMapper<SaleSeries> {
    @Select("SELECT ss.sale_series_id, ss.shop_id, ss.series_id, ss.sale_title, ss.sale_description, " +
            "ss.sale_cover_image, ss.sale_status, ss.created_at, ss.updated_at, " +
            "(SELECT COUNT(*) FROM sale_variant sv WHERE sv.sale_series_id = ss.sale_series_id) AS variant_count, " +
            "MIN(sv.sale_price) AS minPrice, MAX(sv.sale_price) AS maxPrice, " +
            "COALESCE((" +
            "  SELECT SUM(oi.quantity) FROM order_items oi " +
            "  JOIN orders o ON oi.order_id = o.order_id COLLATE utf8mb4_unicode_ci " +
            "  WHERE oi.product_id IN (" +
            "    SELECT sv2.sale_variant_id FROM sale_variant sv2 WHERE sv2.sale_series_id = ss.sale_series_id COLLATE utf8mb4_unicode_ci" +
            "  ) AND o.order_status NOT IN ('CANCELLED', 'REFUNDED')" +
            "), 0) AS totalSales, " +
            "s.shop_name AS shopName " +
            "FROM sale_series ss " +
            "LEFT JOIN sale_variant sv ON ss.sale_series_id = sv.sale_series_id COLLATE utf8mb4_unicode_ci " +
            "LEFT JOIN shop s ON ss.shop_id = s.shop_id COLLATE utf8mb4_unicode_ci " +
            "WHERE ss.shop_id = #{shopId} " +
            "GROUP BY ss.sale_series_id, ss.shop_id, ss.series_id, ss.sale_title, ss.sale_description, " +
            "ss.sale_cover_image, ss.sale_status, ss.created_at, ss.updated_at, s.shop_name")
    List<SaleSeries> selectSaleSeriesWithPriceByShopId(@Param("shopId") String shopId);

    @Select("SELECT ss.sale_series_id, ss.shop_id, ss.series_id, ss.sale_title, ss.sale_description, " +
            "ss.sale_cover_image, ss.sale_status, ss.created_at, ss.updated_at, " +
            "(SELECT COUNT(*) FROM sale_variant sv WHERE sv.sale_series_id = ss.sale_series_id) AS variant_count, " +
            "MIN(sv.sale_price) AS minPrice, MAX(sv.sale_price) AS maxPrice, " +
            "COALESCE((" +
            "  SELECT SUM(oi.quantity) FROM order_items oi " +
            "  JOIN orders o ON oi.order_id = o.order_id COLLATE utf8mb4_unicode_ci " +
            "  WHERE oi.product_id IN (" +
            "    SELECT sv2.sale_variant_id FROM sale_variant sv2 WHERE sv2.sale_series_id = ss.sale_series_id COLLATE utf8mb4_unicode_ci" +
            "  ) AND o.order_status NOT IN ('CANCELLED', 'REFUNDED')" +
            "), 0) AS totalSales, " +
            "s.shop_name AS shopName " +
            "FROM sale_series ss " +
            "LEFT JOIN sale_variant sv ON ss.sale_series_id = sv.sale_series_id COLLATE utf8mb4_unicode_ci " +
            "LEFT JOIN shop s ON ss.shop_id = s.shop_id COLLATE utf8mb4_unicode_ci " +
            "GROUP BY ss.sale_series_id, ss.shop_id, ss.series_id, ss.sale_title, ss.sale_description, " +
            "ss.sale_cover_image, ss.sale_status, ss.created_at, ss.updated_at, s.shop_name")
    List<SaleSeries> selectAllSaleSeriesWithPrice();

    @Select("SELECT ss.sale_series_id, ss.shop_id, ss.series_id, ss.sale_title, ss.sale_description, " +
            "ss.sale_cover_image, ss.sale_status, ss.custom_images, ss.created_at, ss.updated_at, " +
            "(SELECT COUNT(*) FROM sale_variant sv WHERE sv.sale_series_id = ss.sale_series_id) AS variant_count, " +
            "MIN(sv.sale_price) AS minPrice, MAX(sv.sale_price) AS maxPrice, " +
            "COALESCE((" +
            "  SELECT SUM(oi.quantity) FROM order_items oi " +
            "  JOIN orders o ON oi.order_id = o.order_id COLLATE utf8mb4_unicode_ci " +
            "  WHERE oi.product_id IN (" +
            "    SELECT sv2.sale_variant_id FROM sale_variant sv2 WHERE sv2.sale_series_id = ss.sale_series_id COLLATE utf8mb4_unicode_ci" +
            "  ) AND o.order_status NOT IN ('CANCELLED', 'REFUNDED')" +
            "), 0) AS totalSales, " +
            "s.shop_name AS shopName, " +
            "series.theme AS theme " +
            "FROM sale_series ss " +
            "LEFT JOIN sale_variant sv ON ss.sale_series_id = sv.sale_series_id COLLATE utf8mb4_unicode_ci " +
            "LEFT JOIN shop s ON ss.shop_id = s.shop_id COLLATE utf8mb4_unicode_ci " +
            "LEFT JOIN series ON ss.series_id = series.series_id " +
            "WHERE ss.sale_series_id = #{saleSeriesId} " +
            "GROUP BY ss.sale_series_id, ss.shop_id, ss.series_id, ss.sale_title, ss.sale_description, " +
            "ss.sale_cover_image, ss.sale_status, ss.custom_images, ss.created_at, ss.updated_at, s.shop_name, series.theme")
    SaleSeries selectSaleSeriesDetailById(@Param("saleSeriesId") String saleSeriesId);
}
