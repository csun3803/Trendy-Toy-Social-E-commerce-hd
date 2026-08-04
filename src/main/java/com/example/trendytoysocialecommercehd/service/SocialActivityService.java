package com.example.trendytoysocialecommercehd.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.trendytoysocialecommercehd.entity.SocialActivity;
import java.util.List;
import java.util.Map;

public interface SocialActivityService {
    Page<SocialActivity> getPublicActivities(int page, int size, String activityType, String userId);
    Page<SocialActivity> getMyActivities(String userId, int page, int size, String publishStatus, String activityType);
    SocialActivity getActivityById(String activityId);
    SocialActivity getActivityByIdWithReportStatus(String activityId, String currentUserId);
    SocialActivity createActivity(String userId, SocialActivity activity);
    SocialActivity updateActivity(String userId, SocialActivity activity);
    void deleteActivity(String userId, String activityId);
    void likeActivity(String userId, String activityId);
    void unlikeActivity(String userId, String activityId);
    Page<SocialActivity> getAdminActivities(int page, int size, String auditStatus, String publishStatus, String activityType);
    void auditActivity(String activityId, String auditorId, String auditStatus, String auditNotes);
    void adminDeleteActivity(String activityId);
    Map<String, Long> getActivityStats();
    void incrementCommentCount(String activityId);
    boolean hasUserReported(String userId, String targetType, String targetId);
}