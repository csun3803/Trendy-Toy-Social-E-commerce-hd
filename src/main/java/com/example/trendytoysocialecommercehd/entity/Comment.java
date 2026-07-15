package com.example.trendytoysocialecommercehd.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import java.util.Date;

@Data
@TableName("comment")
public class Comment {
    @TableId(type = IdType.ASSIGN_UUID)
    private String commentId;
    private String activityId;
    private String userId;
    private String parentCommentId;
    private String rootCommentId;
    private String content;
    private String auditStatus;
    private String auditNotes;
    private String auditorId;
    private Date auditedAt;
    private Integer likeCount;
    private Integer replyCount;
    private Date commentedAt;
    private String ipAddress;
    private String location;

    @TableField(exist = false)
    private UserInfo userInfo;

    @TableField(exist = false)
    private UserInfo replyToUserInfo;

    @TableField(exist = false)
    private java.util.List<Comment> replies;

    @Data
    public static class UserInfo {
        private String userId;
        private String username;
        private String avatarUrl;
    }
}