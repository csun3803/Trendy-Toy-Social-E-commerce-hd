package com.example.trendytoysocialecommercehd.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.trendytoysocialecommercehd.entity.BlindBoxSet;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 抽盒机套盒 Mapper
 */
@Mapper
public interface BlindBoxSetMapper extends BaseMapper<BlindBoxSet> {

    /** 查询抽盒机下所有套盒（按序号排序） */
    @Select("SELECT * FROM blind_box_set WHERE machine_id = #{machineId} ORDER BY set_index ASC")
    List<BlindBoxSet> selectByMachineId(@Param("machineId") String machineId);

    /** 查询抽盒机下活跃的套盒（有未售格位的） */
    @Select("SELECT * FROM blind_box_set WHERE machine_id = #{machineId} AND status = 'ACTIVE' ORDER BY set_index ASC")
    List<BlindBoxSet> selectActiveByMachineId(@Param("machineId") String machineId);

    /** 查询抽盒机的最大套盒序号 */
    @Select("SELECT COALESCE(MAX(set_index), -1) FROM blind_box_set WHERE machine_id = #{machineId}")
    Integer selectMaxSetIndex(@Param("machineId") String machineId);

    /** 更新套盒已售数和状态 */
    @Update("UPDATE blind_box_set SET sold_count = sold_count + 1, " +
            "status = CASE WHEN sold_count + 1 >= total_slots THEN 'COMPLETED' ELSE status END, " +
            "updated_at = NOW() WHERE set_id = #{setId}")
    int incrementSoldCount(@Param("setId") String setId);
}
