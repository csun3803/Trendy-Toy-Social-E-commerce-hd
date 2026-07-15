package com.example.trendytoysocialecommercehd.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.trendytoysocialecommercehd.entity.Shop;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;

@Mapper
public interface ShopMapper extends BaseMapper<Shop> {

    // 查询店铺月销量
    @Select("SELECT COALESCE(SUM(oi.quantity), 0) FROM order_items oi " +
            "JOIN orders o ON oi.order_id = o.order_id " +
            "WHERE oi.item_seller_id = #{shopId} " +
            "AND o.order_status IN ('PENDING_SHIPMENT','SHIPPED','COMPLETED') " +
            "AND o.create_time >= DATE_FORMAT(NOW(), '%Y-%m-01')")
    Integer getMonthlySales(@Param("shopId") String shopId);

    @Select("SELECT COALESCE(SUM(oi.quantity), 0) FROM order_items oi " +
            "JOIN orders o ON oi.order_id = o.order_id " +
            "WHERE oi.item_seller_id = #{shopId} " +
            "AND o.order_status IN ('PENDING_SHIPMENT','SHIPPED','COMPLETED')")
    Integer getTotalSales(@Param("shopId") String shopId);

    @Select("SELECT COALESCE(SUM(oi.actual_subtotal), 0) FROM order_items oi " +
            "JOIN orders o ON oi.order_id = o.order_id " +
            "WHERE oi.item_seller_id = #{shopId} " +
            "AND o.order_status IN ('PENDING_SHIPMENT','SHIPPED','COMPLETED')")
    BigDecimal getTotalSalesAmount(@Param("shopId") String shopId);
}