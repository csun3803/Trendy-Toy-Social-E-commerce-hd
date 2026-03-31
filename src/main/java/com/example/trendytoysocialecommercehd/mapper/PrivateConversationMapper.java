package com.example.trendytoysocialecommercehd.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.trendytoysocialecommercehd.entity.PrivateConversation;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface PrivateConversationMapper extends BaseMapper<PrivateConversation> {
    @Select("SELECT * FROM private_conversation WHERE user_a_id = #{userId} OR user_b_id = #{userId} ORDER BY last_message_at DESC")
    List<PrivateConversation> findConversationsByUserId(String userId);
}
