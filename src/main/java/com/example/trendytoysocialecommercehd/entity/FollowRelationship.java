package com.example.trendytoysocialecommercehd.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("follow_relationship")
public class FollowRelationship {
    private String followerId;
    private String followingId;
    private Date createdAt;
}