package com.example.trendytoysocialecommercehd.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.trendytoysocialecommercehd.entity.SocialActivity;
import java.util.List;

public interface SocialActivityService {
    Page<SocialActivity> getPublicActivities(int page, int size, String activityType);
    Page<SocialActivity> getMyActivities(String userId, int page, int size, String publishStatus);
    SocialActivity getActivityById(String activityId);
    SocialActivity createActivity(String userId, SocialActivity activity);
    SocialActivity updateActivity(String userId, SocialActivity activity);
    void deleteActivity(String userId, String activityId);
    void likeActivity(String userId, String activityId);
    void unlikeActivity(String userId, String activityId);
}