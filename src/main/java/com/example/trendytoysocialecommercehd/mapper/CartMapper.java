package com.example.trendytoysocialecommercehd.mapper;

        import com.baomidou.mybatisplus.core.mapper.BaseMapper;
        import com.example.trendytoysocialecommercehd.entity.CartItem;
        import org.apache.ibatis.annotations.Delete;
        import org.apache.ibatis.annotations.Mapper;
        import org.apache.ibatis.annotations.Param;
        import org.apache.ibatis.annotations.Update;

@Mapper
public interface CartMapper extends BaseMapper<CartItem> {

    @Delete("DELETE FROM cart_item WHERE user_id = #{userId} AND is_selected = 1")
    int deleteSelectedByUserId(@Param("userId") String userId);

    @Delete("DELETE FROM cart_item WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") String userId);

    @Update("UPDATE cart_item SET is_selected = #{isSelected}, updated_at = NOW() WHERE user_id = #{userId}")
    int updateAllSelected(@Param("userId") String userId, @Param("isSelected") Boolean isSelected);
}