package com.example.trendytoysocialecommercehd.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.trendytoysocialecommercehd.entity.MyDisplayCabinet;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface MyDisplayCabinetMapper extends BaseMapper<MyDisplayCabinet> {

    @Select("SELECT * FROM my_display_cabinet WHERE user_id = #{userId} ORDER BY created_at DESC")
    List<MyDisplayCabinet> selectByUserId(String userId);

    @Select("SELECT COUNT(*) FROM cabinet_item WHERE cabinet_id = #{cabinetId}")
    int countItemsByCabinetId(String cabinetId);
}