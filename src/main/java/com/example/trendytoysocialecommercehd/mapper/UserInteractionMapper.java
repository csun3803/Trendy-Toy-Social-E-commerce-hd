  package com.example.trendytoysocialecommercehd.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.trendytoysocialecommercehd.entity.UserInteraction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface UserInteractionMapper extends BaseMapper<UserInteraction> {

    @Select("SELECT * FROM user_interaction WHERE user_id = #{userId} AND target_type = #{targetType} AND target_id = #{targetId} AND action_type = #{actionType} LIMIT 1")
    UserInteraction findByUserAndTarget(@Param("userId") String userId, @Param("targetType") String targetType, @Param("targetId") String targetId, @Param("actionType") String actionType);

    @Select("SELECT COUNT(*) FROM user_interaction WHERE target_type = #{targetType} AND target_id = #{targetId} AND action_type = #{actionType} AND status = 'ACTIVE'")
    int countActiveInteractions(@Param("targetType") String targetType, @Param("targetId") String targetId, @Param("actionType") String actionType);

    @Select("SELECT DISTINCT user_id FROM user_interaction WHERE target_type = 'USER' AND target_id = #{userId} AND action_type = 'FOLLOW' AND status = 'ACTIVE'")
    List<String> findFollowerIds(@Param("userId") String userId);

    @Select("SELECT DISTINCT target_id FROM user_interaction WHERE user_id = #{userId} AND target_type = 'USER' AND action_type = 'FOLLOW' AND status = 'ACTIVE'")
    List<String> findFollowingIds(@Param("userId") String userId);

    @Select("SELECT DISTINCT target_id FROM user_interaction WHERE user_id = #{userId} AND target_type = #{targetType} AND action_type = 'FAVORITE' AND status = 'ACTIVE'")
    List<String> findFavoriteTargetIds(@Param("userId") String userId, @Param("targetType") String targetType);

    @Select("SELECT DISTINCT target_id FROM user_interaction WHERE user_id = #{userId} AND target_type = #{targetType} AND action_type = 'LIKE' AND status = 'ACTIVE'")
    List<String> findLikedTargetIds(@Param("userId") String userId, @Param("targetType") String targetType);

    // 通知：点赞我的动态（target_id 在指定列表中）
    @Select("<script>" +
            "SELECT i.*, u.username, u.avatar_url FROM user_interaction i " +
            "LEFT JOIN user u ON i.user_id COLLATE utf8mb4_unicode_ci = u.user_id " +
            "WHERE i.target_type = 'ACTIVITY' AND i.action_type = 'LIKE' AND i.status = 'ACTIVE' " +
            "AND i.user_id != #{currentUserId} " +
            "AND i.target_id IN " +
            "<foreach item='id' collection='activityIds' open='(' separator=',' close=')'>#{id}</foreach> " +
            "ORDER BY i.created_at DESC" +
            "</script>")
    List<java.util.Map<String, Object>> findLikeNotifications(@Param("currentUserId") String currentUserId, @Param("activityIds") List<String> activityIds);

    // 通知：关注我
    @Select("SELECT i.*, u.username, u.avatar_url FROM user_interaction i " +
            "LEFT JOIN user u ON i.user_id COLLATE utf8mb4_unicode_ci = u.user_id " +
            "WHERE i.target_type = 'USER' AND i.action_type = 'FOLLOW' AND i.status = 'ACTIVE' " +
            "AND i.target_id = #{currentUserId} " +
            "ORDER BY i.created_at DESC")
    List<java.util.Map<String, Object>> findFollowNotifications(@Param("currentUserId") String currentUserId);
}
