package com.example.trendytoysocialecommercehd.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.trendytoysocialecommercehd.entity.Comment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Map;

@Mapper
public interface CommentMapper extends BaseMapper<Comment> {
    IPage<Comment> selectCommentsByActivityId(Page<Comment> page, @Param("activityId") String activityId);

    // 通知：评论我的动态
    @Select("<script>" +
            "SELECT c.*, u.username, u.avatar_url, a.title as activity_title " +
            "FROM comment c " +
            "LEFT JOIN user u ON c.user_id COLLATE utf8mb4_unicode_ci = u.user_id " +
            "LEFT JOIN social_activity a ON c.activity_id = a.activity_id " +
            "WHERE c.audit_status = '已通过' " +
            "AND c.activity_id IN " +
            "<foreach item='id' collection='activityIds' open='(' separator=',' close=')'>#{id}</foreach> " +
            "ORDER BY c.commented_at DESC" +
            "</script>")
    List<Map<String, Object>> findCommentNotifications(@Param("activityIds") List<String> activityIds);
}