package com.example.trendytoysocialecommercehd.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.trendytoysocialecommercehd.entity.BlindBoxSlot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface BlindBoxSlotMapper extends BaseMapper<BlindBoxSlot> {

    @Select("SELECT * FROM blind_box_slot WHERE machine_id = #{machineId} ORDER BY slot_no ASC")
    List<BlindBoxSlot> selectByMachineId(String machineId);

    @Select("SELECT * FROM blind_box_slot WHERE set_id = #{setId} ORDER BY slot_no ASC")
    List<BlindBoxSlot> selectBySetId(String setId);

    @Select("SELECT COUNT(*) FROM blind_box_slot WHERE set_id = #{setId} AND status = 'AVAILABLE'")
    int countAvailableSlotsBySetId(String setId);

    @Select("SELECT COUNT(*) FROM blind_box_slot WHERE machine_id = #{machineId} AND status = 'AVAILABLE'")
    int countAvailableSlots(String machineId);

    @Select("SELECT COUNT(*) FROM blind_box_slot WHERE machine_id = #{machineId} AND status = 'SOLD'")
    int countSoldSlots(String machineId);

    @Update("UPDATE blind_box_slot SET status = 'SOLD' WHERE slot_id = #{slotId}")
    int markAsSold(String slotId);
}
