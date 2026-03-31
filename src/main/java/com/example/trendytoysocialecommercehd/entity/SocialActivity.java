package com.example.trendytoysocialecommercehd.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import java.util.Date;

@Data
@TableName("social_activity")
public class SocialActivity {
    @TableId(type = IdType.ASSIGN_UUID)
    private String activityId;
    private String userId;
    private String activityType;
    private String title;
    private String content;
    private String coverImage;
    private String imageList;
    private String location;
    private String publishStatus;
    private String auditStatus;
    private String auditNotes;
    private String auditorId;
    private Date auditedAt;
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private Integer favoriteCount;
    private Integer shareCount;
    private Date publishedAt;
    private Date updatedAt;

    @TableField(exist = false)
    private UserInfo userInfo;

    @Data
    public static class UserInfo {
        private String userId;
        private String username;
        private String avatarUrl;
    }
}