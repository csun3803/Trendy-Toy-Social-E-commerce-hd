package com.example.trendytoysocialecommercehd.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.trendytoysocialecommercehd.entity.BlindBoxQueue;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface BlindBoxQueueMapper extends BaseMapper<BlindBoxQueue> {

    @Select("SELECT * FROM blind_box_queue WHERE machine_id = #{machineId} AND status IN ('WAITING','ACTIVE') ORDER BY queue_position ASC")
    List<BlindBoxQueue> selectActiveQueue(String machineId);

    @Select("SELECT COUNT(*) FROM blind_box_queue WHERE machine_id = #{machineId} AND status IN ('WAITING','ACTIVE')")
    int countActiveQueue(String machineId);

    @Select("SELECT * FROM blind_box_queue WHERE machine_id = #{machineId} AND user_id = #{userId} AND status IN ('WAITING','ACTIVE')")
    BlindBoxQueue selectUserActiveQueue(String machineId, String userId);

    @Update("UPDATE blind_box_queue SET status = 'ACTIVE' WHERE queue_id = #{queueId}")
    int activateUser(String queueId);

    @Update("UPDATE blind_box_queue SET status = 'LEFT', left_at = NOW() WHERE machine_id = #{machineId} AND user_id = #{userId} AND status IN ('WAITING','ACTIVE')")
    int leaveQueue(String machineId, String userId);
}
