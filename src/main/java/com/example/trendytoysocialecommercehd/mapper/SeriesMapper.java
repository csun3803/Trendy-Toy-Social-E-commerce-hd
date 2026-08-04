package com.example.trendytoysocialecommercehd.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.trendytoysocialecommercehd.dto.SeriesDetailDTO;
import com.example.trendytoysocialecommercehd.entity.Series;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface SeriesMapper extends BaseMapper<Series> {

    /**
     * 查询系列列表（包含销量和实际款式数，支持IP过滤）
     * 款式数量改为从product表实时统计
     */
    @Select("<script>" +
            "SELECT s.series_id, s.series_name, s.ip_album_id, s.theme, s.description, s.cover_image, " +
            "s.is_limited, s.limited_quantity, s.min_price, s.fullset_price, s.series_hotness, " +
            "s.start_date, s.end_date, s.status, s.create_time, s.update_time, " +
            "(SELECT SUM(od.quantity) FROM order_items od " +
            "JOIN product p ON od.product_id = p.product_id " +
            "WHERE p.series_id = s.series_id) as sales_count, " +
            "(SELECT COUNT(*) FROM product p WHERE p.series_id = s.series_id) as actual_variant_count, " +
            "(SELECT COUNT(*) FROM product p WHERE p.series_id = s.series_id AND p.is_hidden_variant = 0) as regular_variants, " +
            "(SELECT COUNT(*) FROM product p WHERE p.series_id = s.series_id AND p.is_hidden_variant = 1) as hidden_variants, " +
            "(SELECT COUNT(*) FROM product p WHERE p.series_id = s.series_id) as total_variants " +
            "FROM series s " +
            "WHERE 1=1 " +
            "<if test='keyword != null and keyword != \"\"'>" +
            " AND (s.series_name LIKE CONCAT('%', #{keyword}, '%') " +
            "   OR s.theme LIKE CONCAT('%', #{keyword}, '%') " +
            "   OR s.description LIKE CONCAT('%', #{keyword}, '%')) " +
            "</if>" +
            "<if test='ipAlbumId != null and ipAlbumId != \"\"'>" +
            " AND s.ip_album_id = #{ipAlbumId} " +
            "</if>" +
            "</script>")
    IPage<Series> selectSeriesPageWithSales(IPage<Series> page, @Param("keyword") String keyword, @Param("ipAlbumId") String ipAlbumId);

    /**
     * 查询系列列表（包含销量和实际款式数）- 兼容旧方法
     * 款式数量改为从product表实时统计
     */
    @Select("SELECT s.series_id, s.series_name, s.ip_album_id, s.theme, s.description, s.cover_image, " +
            "s.is_limited, s.limited_quantity, s.min_price, s.fullset_price, s.series_hotness, " +
            "s.start_date, s.end_date, s.status, s.create_time, s.update_time, " +
            "(SELECT SUM(od.quantity) FROM order_items od " +
            "JOIN product p ON od.product_id = p.product_id " +
            "WHERE p.series_id = s.series_id) as sales_count, " +
            "(SELECT COUNT(*) FROM product p WHERE p.series_id = s.series_id) as actual_variant_count, " +
            "(SELECT COUNT(*) FROM product p WHERE p.series_id = s.series_id AND p.is_hidden_variant = 0) as regular_variants, " +
            "(SELECT COUNT(*) FROM product p WHERE p.series_id = s.series_id AND p.is_hidden_variant = 1) as hidden_variants, " +
            "(SELECT COUNT(*) FROM product p WHERE p.series_id = s.series_id) as total_variants " +
            "FROM series s " +
            "WHERE 1=1 " +
            "AND (#{keyword} IS NULL OR " +
            "     s.series_name LIKE CONCAT('%', #{keyword}, '%') OR " +
            "     s.theme LIKE CONCAT('%', #{keyword}, '%') OR " +
            "     s.description LIKE CONCAT('%', #{keyword}, '%')) ")
    IPage<Series> selectSeriesPageWithSalesLegacy(IPage<Series> page, @Param("keyword") String keyword);

    /**
     * 查询系列详情（包含销量、实际款式数和商品列表）
     * 款式数量改为从product表实时统计
     */
    @Select("SELECT s.series_id, s.series_name, s.ip_album_id, s.theme, s.description, s.cover_image, " +
            "s.is_limited, s.limited_quantity, s.min_price, s.fullset_price, s.series_hotness, " +
            "s.start_date, s.end_date, s.status, s.create_time, s.update_time, " +
            "(SELECT COUNT(*) FROM product p WHERE p.series_id = s.series_id) as sales_count, " +
            "(SELECT COUNT(*) FROM product p WHERE p.series_id = s.series_id) as actual_variant_count, " +
            "(SELECT COUNT(*) FROM product p WHERE p.series_id = s.series_id AND p.is_hidden_variant = 0) as regular_variants, " +
            "(SELECT COUNT(*) FROM product p WHERE p.series_id = s.series_id AND p.is_hidden_variant = 1) as hidden_variants, " +
            "(SELECT COUNT(*) FROM product p WHERE p.series_id = s.series_id) as total_variants " +
            "FROM series s " +
            "WHERE s.series_id = #{seriesId}")
    @Results({
            @Result(property = "seriesId", column = "series_id"),
            @Result(property = "seriesName", column = "series_name"),
            @Result(property = "ipAlbumId", column = "ip_album_id"),
            @Result(property = "theme", column = "theme"),
            @Result(property = "description", column = "description"),
            @Result(property = "coverImage", column = "cover_image"),
            @Result(property = "regularVariants", column = "regular_variants"),
            @Result(property = "hiddenVariants", column = "hidden_variants"),
            @Result(property = "totalVariants", column = "total_variants"),
            @Result(property = "isLimited", column = "is_limited"),
            @Result(property = "limitedQuantity", column = "limited_quantity"),
            @Result(property = "minPrice", column = "min_price"),
            @Result(property = "fullsetPrice", column = "fullset_price"),
            @Result(property = "seriesHotness", column = "series_hotness"),
            @Result(property = "startDate", column = "start_date"),
            @Result(property = "endDate", column = "end_date"),
            @Result(property = "status", column = "status"),
            @Result(property = "salesCount", column = "sales_count"),
            @Result(property = "createTime", column = "create_time"),
            @Result(property = "updateTime", column = "update_time"),
            @Result(property = "actualVariantCount", column = "actual_variant_count"),
            @Result(property = "products", column = "series_id",
                    many = @Many(select = "com.example.trendytoysocialecommercehd.mapper.ProductMapper.selectProductsBySeriesId"))
    })
    SeriesDetailDTO selectSeriesDetail(@Param("seriesId") String seriesId);

    /**
     * 查询单个系列（包含销量和实际款式数）- 用于兼容旧方法
     * 款式数量改为从product表实时统计
     */
    @Select("SELECT s.series_id, s.series_name, s.ip_album_id, s.theme, s.description, s.cover_image, " +
            "s.is_limited, s.limited_quantity, s.min_price, s.fullset_price, s.series_hotness, " +
            "s.start_date, s.end_date, s.status, s.create_time, s.update_time, " +
            "COALESCE((SELECT SUM(od.quantity) FROM order_items od " +
            "JOIN product p ON od.product_id = p.product_id " +
            "WHERE p.series_id = #{seriesId}), 0) as sales_count, " +
            "(SELECT COUNT(*) FROM product p WHERE p.series_id = #{seriesId}) as actual_variant_count, " +
            "(SELECT COUNT(*) FROM product p WHERE p.series_id = #{seriesId} AND p.is_hidden_variant = 0) as regular_variants, " +
            "(SELECT COUNT(*) FROM product p WHERE p.series_id = #{seriesId} AND p.is_hidden_variant = 1) as hidden_variants, " +
            "(SELECT COUNT(*) FROM product p WHERE p.series_id = #{seriesId}) as total_variants " +
            "FROM series s " +
            "WHERE s.series_id = #{seriesId}")
    Series selectSeriesWithSales(@Param("seriesId") String seriesId);

    /**
     * 按名称/主题/描述模糊查询系列摘要信息，供AI工具接口使用
     * 返回字段: seriesId, seriesName, theme, coverImage, totalVariants,
     *          regularVariants, hiddenVariants, minPrice, fullsetPrice,
     *          isLimited, seriesHotness, status, description
     * 款式数量改为从product表实时统计
     */
    @Select("SELECT s.series_id AS seriesId, s.series_name AS seriesName, s.theme, " +
            "s.description, s.cover_image AS coverImage, " +
            "(SELECT COUNT(*) FROM product p WHERE p.series_id = s.series_id AND p.is_hidden_variant = 0) AS regularVariants, " +
            "(SELECT COUNT(*) FROM product p WHERE p.series_id = s.series_id AND p.is_hidden_variant = 1) AS hiddenVariants, " +
            "(SELECT COUNT(*) FROM product p WHERE p.series_id = s.series_id) AS totalVariants, " +
            "s.min_price AS minPrice, " +
            "s.fullset_price AS fullsetPrice, s.is_limited AS isLimited, " +
            "s.series_hotness AS seriesHotness, s.status " +
            "FROM series s " +
            "WHERE s.series_name LIKE CONCAT('%', #{seriesName}, '%') " +
            "   OR s.theme LIKE CONCAT('%', #{seriesName}, '%') " +
            "   OR s.description LIKE CONCAT('%', #{seriesName}, '%') " +
            "ORDER BY s.series_hotness DESC " +
            "LIMIT 10")
    List<java.util.Map<String, Object>> selectSeriesInfoByName(@Param("seriesName") String seriesName);
}