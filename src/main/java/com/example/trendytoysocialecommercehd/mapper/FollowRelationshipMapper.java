package com.example.trendytoysocialecommercehd.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.trendytoysocialecommercehd.entity.FollowRelationship;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface FollowRelationshipMapper extends BaseMapper<FollowRelationship> {

    @Select("SELECT COUNT(*) FROM follow_relationship WHERE follower_id = #{userId}")
    int countFollowing(String userId);

    @Select("SELECT COUNT(*) FROM follow_relationship WHERE following_id = #{userId}")
    int countFollower(String userId);
}