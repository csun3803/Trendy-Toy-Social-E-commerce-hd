package com.example.trendytoysocialecommercehd.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.trendytoysocialecommercehd.entity.CabinetItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface CabinetItemMapper extends BaseMapper<CabinetItem> {

    @Select("SELECT ci.*, p.name, p.image_url as product_image " +
            "FROM cabinet_item ci " +
            "LEFT JOIN product p ON ci.product_id = p.product_id " +
            "WHERE ci.cabinet_id = #{cabinetId} " +
            "ORDER BY ci.added_at DESC")
    List<CabinetItem> selectByCabinetId(String cabinetId);
}
