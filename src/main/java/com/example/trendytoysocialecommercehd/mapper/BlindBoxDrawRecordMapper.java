package com.example.trendytoysocialecommercehd.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.trendytoysocialecommercehd.entity.BlindBoxDrawRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Mapper
public interface BlindBoxDrawRecordMapper extends BaseMapper<BlindBoxDrawRecord> {

    /**
     * 统计用户在某个抽盒机的非隐藏款抽取次数（用于保底机制）
     */
    @Select("SELECT COUNT(*) FROM blind_box_draw_record " +
            "WHERE machine_id = #{machineId} AND user_id = #{userId} AND is_hidden = 0")
    int countUserNonHiddenDraws(String machineId, String userId);

    /**
     * 统计抽盒机总抽数
     */
    @Select("SELECT COUNT(*) FROM blind_box_draw_record WHERE machine_id = #{machineId}")
    int countByMachineId(@Param("machineId") String machineId);

    /**
     * 统计抽盒机累计流水（单抽价格之和）
     */
    @Select("SELECT COALESCE(SUM(draw_price), 0) FROM blind_box_draw_record WHERE machine_id = #{machineId}")
    BigDecimal sumRevenueByMachineId(@Param("machineId") String machineId);

    /**
     * 统计抽盒机参与用户数
     */
    @Select("SELECT COUNT(DISTINCT user_id) FROM blind_box_draw_record WHERE machine_id = #{machineId}")
    int countDistinctUsers(@Param("machineId") String machineId);

    /**
     * 统计抽盒机保底触发次数
     */
    @Select("SELECT COUNT(*) FROM blind_box_draw_record WHERE machine_id = #{machineId} AND is_guaranteed = 1")
    int countGuaranteedDraws(@Param("machineId") String machineId);

    /**
     * 统计抽盒机各款式抽出次数（用于数据页款式占比统计）
     * 返回字段: saleVariantId, drawCount, isHidden, variantName, variantImage
     */
    @Select("SELECT r.sale_variant_id AS saleVariantId, " +
            "       COUNT(*) AS drawCount, " +
            "       MAX(r.is_hidden) AS isHidden, " +
            "       sv.custom_description AS variantName, " +
            "       sv.custom_images AS variantImage " +
            "FROM blind_box_draw_record r " +
            "LEFT JOIN sale_variant sv ON r.sale_variant_id = sv.sale_variant_id " +
            "WHERE r.machine_id = #{machineId} " +
            "GROUP BY r.sale_variant_id, sv.custom_description, sv.custom_images " +
            "ORDER BY drawCount DESC")
    List<Map<String, Object>> countVariantDrawStats(@Param("machineId") String machineId);

    /**
     * 分页查询抽盒机的抽盒记录（含订单号、款式名、用户ID）
     */
    @Select("<script>" +
            "SELECT r.*, " +
            "       sv.custom_description AS variantName, " +
            "       sv.custom_images AS variantImage " +
            "FROM blind_box_draw_record r " +
            "LEFT JOIN sale_variant sv ON r.sale_variant_id = sv.sale_variant_id " +
            "WHERE r.machine_id = #{machineId} " +
            "<if test='userId != null and userId != \"\"'>" +
            "  AND r.user_id = #{userId} " +
            "</if>" +
            "<if test='drawType != null and drawType != \"\"'>" +
            "  AND r.draw_type = #{drawType} " +
            "</if>" +
            "ORDER BY r.created_at DESC" +
            "</script>")
    List<BlindBoxDrawRecord> selectMachineRecords(
            @Param("machineId") String machineId,
            @Param("userId") String userId,
            @Param("drawType") String drawType);

    /**
     * 查询用户所有抽盒记录（含款式名、图片、机器名）
     */
    @Select("SELECT r.*, " +
            "       sv.custom_description AS variantName, " +
            "       sv.custom_images AS variantImage, " +
            "       bm.machine_name AS machineName " +
            "FROM blind_box_draw_record r " +
            "LEFT JOIN sale_variant sv ON r.sale_variant_id = sv.sale_variant_id " +
            "LEFT JOIN blind_box_machine bm ON r.machine_id = bm.machine_id " +
            "WHERE r.user_id = #{userId} " +
            "ORDER BY r.created_at DESC")
    List<BlindBoxDrawRecord> selectUserRecords(@Param("userId") String userId);

    /**
     * 欧气排行榜：按隐藏款数量降序
     */
    @Select("SELECT r.user_id AS userId, " +
            "       u.username AS username, " +
            "       u.avatar_url AS avatarUrl, " +
            "       COUNT(*) AS totalDraws, " +
            "       SUM(CASE WHEN r.is_hidden = 1 THEN 1 ELSE 0 END) AS hiddenCount " +
            "FROM blind_box_draw_record r " +
            "LEFT JOIN user u ON r.user_id = u.user_id " +
            "GROUP BY r.user_id, u.username, u.avatar_url " +
            "ORDER BY hiddenCount DESC, totalDraws DESC " +
            "LIMIT #{limit}")
    List<Map<String, Object>> selectLuckRanking(@Param("limit") int limit);
}
