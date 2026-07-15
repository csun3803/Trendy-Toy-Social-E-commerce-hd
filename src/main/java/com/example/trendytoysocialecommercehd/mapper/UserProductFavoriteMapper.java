package com.example.trendytoysocialecommercehd.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.trendytoysocialecommercehd.entity.UserProductFavorite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface UserProductFavoriteMapper extends BaseMapper<UserProductFavorite> {

    /**
     * 查询用户收藏列表（含商品信息），供AI工具接口使用
     */
    @Select("SELECT f.favorite_id AS favoriteId, f.product_id AS productId, f.status, f.created_at AS createdAt, " +
            "p.name AS productName, p.price, p.stock, p.brand, p.status AS productStatus, " +
            "p.image_url AS imageUrl, p.series_id AS seriesId " +
            "FROM user_product_favorite f " +
            "LEFT JOIN product p ON f.product_id = p.product_id COLLATE utf8mb4_unicode_ci " +
            "WHERE f.user_id = #{userId} AND f.status = 'active' " +
            "ORDER BY f.created_at DESC")
    List<Map<String, Object>> selectFavoritesWithProduct(@Param("userId") String userId);
}