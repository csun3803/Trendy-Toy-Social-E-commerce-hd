package com.example.trendytoysocialecommercehd.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.trendytoysocialecommercehd.entity.BlindBoxMachine;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface BlindBoxMachineMapper extends BaseMapper<BlindBoxMachine> {

    /**
     * 获取所有活跃的抽盒机列表（用户端使用，返回 ACTIVE 状态的已审核抽盒机）
     * 兼容旧数据：audit_status 为 APPROVED 或 NULL/DRAFT（旧数据无审核字段）的 ACTIVE 抽盒机都展示
     */
    @Select("SELECT m.*, s.shop_name, " +
            "sr.series_name as series_name " +
            "FROM blind_box_machine m " +
            "LEFT JOIN shop s ON m.shop_id = s.shop_id " +
            "LEFT JOIN series sr ON m.series_id = sr.series_id " +
            "WHERE m.machine_status = 'ACTIVE' " +
            "AND (m.audit_status = 'APPROVED' OR m.audit_status IS NULL OR m.audit_status = 'DRAFT') " +
            "ORDER BY m.sort_order DESC, m.created_at DESC")
    List<BlindBoxMachine> selectActiveMachinesWithInfo();

    /**
     * 获取抽盒机详情（含店铺名和系列名）
     */
    @Select("SELECT m.*, s.shop_name, " +
            "sr.series_name as series_name " +
            "FROM blind_box_machine m " +
            "LEFT JOIN shop s ON m.shop_id = s.shop_id " +
            "LEFT JOIN series sr ON m.series_id = sr.series_id " +
            "WHERE m.machine_id = #{machineId}")
    BlindBoxMachine selectMachineWithInfo(@Param("machineId") String machineId);

    /**
     * 商家端：查询某店铺下的抽盒机列表（带可选筛选条件）
     * 注：使用动态 SQL 实现关键词/状态/审核状态过滤
     */
    @Select("<script>" +
            "SELECT m.*, s.shop_name, " +
            "sr.series_name as series_name " +
            "FROM blind_box_machine m " +
            "LEFT JOIN shop s ON m.shop_id = s.shop_id " +
            "LEFT JOIN series sr ON m.series_id = sr.series_id " +
            "WHERE m.shop_id = #{shopId} " +
            "<if test='keyword != null and keyword != \"\"'>" +
            "  AND (m.machine_name LIKE CONCAT('%', #{keyword}, '%') " +
            "       OR sr.series_name LIKE CONCAT('%', #{keyword}, '%')) " +
            "</if>" +
            "<if test='machineStatus != null and machineStatus != \"\"'>" +
            "  AND m.machine_status = #{machineStatus} " +
            "</if>" +
            "<if test='auditStatus != null and auditStatus != \"\"'>" +
            "  AND m.audit_status = #{auditStatus} " +
            "</if>" +
            "ORDER BY m.created_at DESC" +
            "</script>")
    List<BlindBoxMachine> selectMerchantMachinesWithInfo(
            @Param("shopId") String shopId,
            @Param("keyword") String keyword,
            @Param("machineStatus") String machineStatus,
            @Param("auditStatus") String auditStatus);

    /**
     * 管理员端：全平台抽盒机列表（带可选筛选条件）
     */
    @Select("<script>" +
            "SELECT m.*, s.shop_name, " +
            "sr.series_name as series_name " +
            "FROM blind_box_machine m " +
            "LEFT JOIN shop s ON m.shop_id = s.shop_id " +
            "LEFT JOIN series sr ON m.series_id = sr.series_id " +
            "WHERE 1=1 " +
            "<if test='shopId != null and shopId != \"\"'>" +
            "  AND m.shop_id = #{shopId} " +
            "</if>" +
            "<if test='machineStatus != null and machineStatus != \"\"'>" +
            "  AND m.machine_status = #{machineStatus} " +
            "</if>" +
            "<if test='auditStatus != null and auditStatus != \"\"'>" +
            "  AND m.audit_status = #{auditStatus} " +
            "</if>" +
            "<if test='keyword != null and keyword != \"\"'>" +
            "  AND (m.machine_name LIKE CONCAT('%', #{keyword}, '%') " +
            "       OR s.shop_name LIKE CONCAT('%', #{keyword}, '%') " +
            "       OR sr.series_name LIKE CONCAT('%', #{keyword}, '%')) " +
            "</if>" +
            "ORDER BY m.created_at DESC" +
            "</script>")
    List<BlindBoxMachine> selectAllMachinesWithInfo(
            @Param("shopId") String shopId,
            @Param("machineStatus") String machineStatus,
            @Param("auditStatus") String auditStatus,
            @Param("keyword") String keyword);
}
