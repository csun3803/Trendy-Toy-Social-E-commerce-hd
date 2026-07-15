package com.example.trendytoysocialecommercehd.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.trendytoysocialecommercehd.entity.BlindBoxStorage;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 盲盒暂存柜 Mapper
 */
@Mapper
public interface BlindBoxStorageMapper extends BaseMapper<BlindBoxStorage> {

    /** 查询用户暂存中的记录 */
    @Select("SELECT * FROM blind_box_storage WHERE user_id = #{userId} AND status = 'STORED' ORDER BY stored_at DESC")
    List<BlindBoxStorage> selectStoredByUserId(@Param("userId") String userId);

    /** 查询用户所有记录（含已发货） */
    @Select("SELECT * FROM blind_box_storage WHERE user_id = #{userId} ORDER BY stored_at DESC")
    List<BlindBoxStorage> selectAllByUserId(@Param("userId") String userId);

    /** 统计用户暂存数量 */
    @Select("SELECT COUNT(*) FROM blind_box_storage WHERE user_id = #{userId} AND status = 'STORED'")
    int countStoredByUserId(@Param("userId") String userId);
}
