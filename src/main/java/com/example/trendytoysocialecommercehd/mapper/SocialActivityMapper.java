package com.example.trendytoysocialecommercehd.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.trendytoysocialecommercehd.entity.SocialActivity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface SocialActivityMapper extends BaseMapper<SocialActivity> {

    @Select("SELECT a.*, u.username, u.avatar_url FROM social_activity a " +
            "LEFT JOIN user u ON a.user_id = u.user_id " +
            "WHERE a.publish_status = 'published' AND a.audit_status = '审核通过' " +
            "ORDER BY a.published_at DESC")
    List<SocialActivity> selectPublicActivities();

    @Select("SELECT a.*, u.username, u.avatar_url FROM social_activity a " +
            "LEFT JOIN user u ON a.user_id = u.user_id " +
            "WHERE a.user_id = #{userId} " +
            "ORDER BY a.updated_at DESC")
    List<SocialActivity> selectByUserId(String userId);
}