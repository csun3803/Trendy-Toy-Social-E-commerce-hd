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
     * 查询系列列表（包含销量）
     */
    @Select("SELECT s.*, " +
            "(SELECT SUM(od.quantity) FROM order_items od " +
            "JOIN product p ON od.product_id = p.product_id " +
            "WHERE p.series_id = s.series_id) as sales_count " +
            "FROM series s " +
            "WHERE 1=1 " +
            "AND (#{keyword} IS NULL OR " +
            "     s.series_name LIKE CONCAT('%', #{keyword}, '%') OR " +
            "     s.theme LIKE CONCAT('%', #{keyword}, '%') OR " +
            "     s.description LIKE CONCAT('%', #{keyword}, '%')) ")
    IPage<Series> selectSeriesPageWithSales(IPage<Series> page, @Param("keyword") String keyword);

    /**
     * 查询系列详情（包含销量和商品列表）
     */
    @Select("SELECT s.*, " +
            "COALESCE((SELECT SUM(od.quantity) FROM order_items od " +
            "JOIN product p ON od.product_id = p.product_id " +
            "WHERE p.series_id = #{seriesId}), 0) as sales_count " +
            "FROM series s " +
            "WHERE s.series_id = #{seriesId}")
    @Results({
            @Result(property = "seriesId", column = "series_id"),
            @Result(property = "seriesName", column = "series_name"),
            @Result(property = "ipAlbumId", column = "ip_album_id"),
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
            @Result(property = "createTime", column = "create_time"),
            @Result(property = "updateTime", column = "update_time"),
            @Result(property = "products", column = "series_id",
                    many = @Many(select = "com.example.trendytoysocialecommercehd.mapper.ProductMapper.selectProductsBySeriesId"))
    })
    SeriesDetailDTO selectSeriesDetail(@Param("seriesId") String seriesId);

    /**
     * 查询单个系列（包含销量）- 用于兼容旧方法
     */
    @Select("SELECT s.*, " +
            "COALESCE((SELECT SUM(od.quantity) FROM order_items od " +
            "JOIN product p ON od.product_id = p.product_id " +
            "WHERE p.series_id = #{seriesId}), 0) as sales_count " +
            "FROM series s " +
            "WHERE s.series_id = #{seriesId}")
    Series selectSeriesWithSales(@Param("seriesId") String seriesId);
}