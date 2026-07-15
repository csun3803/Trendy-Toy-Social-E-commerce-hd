package com.example.trendytoysocialecommercehd.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.trendytoysocialecommercehd.entity.BlindBoxMachineVariant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface BlindBoxMachineVariantMapper extends BaseMapper<BlindBoxMachineVariant> {

    /**
     * 查询抽盒机下所有款式覆盖配置（联表 sale_variant 获取默认库存与款式信息）
     */
    @Select("SELECT v.*, " +
            "       sv.custom_description AS variant_name, " +
            "       sv.custom_images AS variant_image, " +
            "       sv.variant_id AS variant_id, " +
            "       sv.stock_quantity AS original_stock " +
            "FROM blind_box_machine_variant v " +
            "LEFT JOIN sale_variant sv ON v.sale_variant_id = sv.sale_variant_id " +
            "WHERE v.machine_id = #{machineId} " +
            "ORDER BY sv.created_at ASC")
    List<BlindBoxMachineVariant> selectByMachineIdWithInfo(@Param("machineId") String machineId);

    /**
     * 查询抽盒机下所有覆盖概率的款式（用于抽盒概率计算）
     */
    @Select("SELECT * FROM blind_box_machine_variant " +
            "WHERE machine_id = #{machineId} AND override_probability = 1")
    List<BlindBoxMachineVariant> selectProbabilityOverrides(@Param("machineId") String machineId);

    /**
     * 查询抽盒机下所有覆盖库存的款式（用于抽盒库存计算）
     */
    @Select("SELECT * FROM blind_box_machine_variant " +
            "WHERE machine_id = #{machineId} AND override_stock = 1")
    List<BlindBoxMachineVariant> selectStockOverrides(@Param("machineId") String machineId);

    /**
     * 删除抽盒机下所有覆盖配置
     */
    @org.apache.ibatis.annotations.Delete("DELETE FROM blind_box_machine_variant WHERE machine_id = #{machineId}")
    int deleteByMachineId(@Param("machineId") String machineId);
}
