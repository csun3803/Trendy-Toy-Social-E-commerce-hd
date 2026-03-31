package com.example.trendytoysocialecommercehd.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.trendytoysocialecommercehd.entity.PrivateMessage;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface PrivateMessageMapper extends BaseMapper<PrivateMessage> {
    @Select("SELECT * FROM private_message WHERE conversation_id = #{conversationId} ORDER BY sent_at ASC")
    List<PrivateMessage> findMessagesByConversationId(String conversationId);

    @Select("SELECT * FROM private_message WHERE receiver_id = #{userId} AND read_at IS NULL")
    List<PrivateMessage> findUnreadMessagesByUserId(String userId);
}
