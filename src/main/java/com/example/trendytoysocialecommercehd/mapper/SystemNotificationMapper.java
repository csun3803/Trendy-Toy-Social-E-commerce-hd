package com.example.trendytoysocialecommercehd.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.trendytoysocialecommercehd.entity.SystemNotification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SystemNotificationMapper extends BaseMapper<SystemNotification> {

    /**
     * 统计用户未读通知数量
     */
    @Select("SELECT COUNT(*) FROM system_notification WHERE user_id = #{userId} AND is_read = false")
    int countUnreadByUserId(@Param("userId") String userId);

    /**
     * 标记用户所有通知为已读
     */
    @Update("UPDATE system_notification SET is_read = true, read_at = NOW() WHERE user_id = #{userId} AND is_read = false")
    int markAllAsRead(@Param("userId") String userId);

    /**
     * 获取用户最新一条通知
     */
    @Select("SELECT * FROM system_notification WHERE user_id = #{userId} ORDER BY created_at DESC LIMIT 1")
    SystemNotification selectLatestByUserId(@Param("userId") String userId);
}
