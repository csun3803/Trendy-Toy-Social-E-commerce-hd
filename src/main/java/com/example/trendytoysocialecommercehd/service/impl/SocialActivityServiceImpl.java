package com.example.trendytoysocialecommercehd.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.trendytoysocialecommercehd.entity.SocialActivity;
import com.example.trendytoysocialecommercehd.entity.User;
import com.example.trendytoysocialecommercehd.mapper.SocialActivityMapper;
import com.example.trendytoysocialecommercehd.mapper.UserMapper;
import com.example.trendytoysocialecommercehd.service.SocialActivityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SocialActivityServiceImpl implements SocialActivityService {

    @Autowired
    private SocialActivityMapper socialActivityMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    public Page<SocialActivity> getPublicActivities(int page, int size, String activityType) {
        Page<SocialActivity> pageObj = new Page<>(page, size);
        QueryWrapper<SocialActivity> wrapper = new QueryWrapper<>();
        wrapper.eq("publish_status", "published")
                .eq("audit_status", "审核通过");
        if (activityType != null && !activityType.isEmpty()) {
            wrapper.eq("activity_type", activityType);
        }
        wrapper.orderByDesc("published_at");

        Page<SocialActivity> result = socialActivityMapper.selectPage(pageObj, wrapper);
        fillUserInfo(result.getRecords());
        return result;
    }

    @Override
    public Page<SocialActivity> getMyActivities(String userId, int page, int size, String publishStatus) {
        Page<SocialActivity> pageObj = new Page<>(page, size);
        QueryWrapper<SocialActivity> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        if (publishStatus != null && !publishStatus.isEmpty()) {
            wrapper.eq("publish_status", publishStatus);
        }
        wrapper.orderByDesc("updated_at");

        Page<SocialActivity> result = socialActivityMapper.selectPage(pageObj, wrapper);
        fillUserInfo(result.getRecords());
        return result;
    }

    @Override
    public SocialActivity getActivityById(String activityId) {
        SocialActivity activity = socialActivityMapper.selectById(activityId);
        if (activity != null) {
            activity.setViewCount(activity.getViewCount() + 1);
            socialActivityMapper.updateById(activity);
            fillUserInfo(Collections.singletonList(activity));
        }
        return activity;
    }

    @Override
    public SocialActivity createActivity(String userId, SocialActivity activity) {
        activity.setActivityId(UUID.randomUUID().toString());
        activity.setUserId(userId);
        activity.setViewCount(0);
        activity.setLikeCount(0);
        activity.setCommentCount(0);
        activity.setFavoriteCount(0);
        activity.setShareCount(0);
        activity.setAuditStatus("待审核");
        activity.setUpdatedAt(new Date());

        if ("published".equals(activity.getPublishStatus())) {
            activity.setPublishedAt(new Date());
        }

        socialActivityMapper.insert(activity);
        return activity;
    }

    @Override
    public SocialActivity updateActivity(String userId, SocialActivity activity) {
        SocialActivity existing = socialActivityMapper.selectById(activity.getActivityId());
        if (existing == null || !existing.getUserId().equals(userId)) {
            throw new RuntimeException("无权修改此动态");
        }

        activity.setUpdatedAt(new Date());
        if ("published".equals(activity.getPublishStatus()) && existing.getPublishedAt() == null) {
            activity.setPublishedAt(new Date());
        }

        socialActivityMapper.updateById(activity);
        return activity;
    }

    @Override
    public void deleteActivity(String userId, String activityId) {
        SocialActivity activity = socialActivityMapper.selectById(activityId);
        if (activity == null || !activity.getUserId().equals(userId)) {
            throw new RuntimeException("无权删除此动态");
        }
        socialActivityMapper.deleteById(activityId);
    }

    @Override
    public void likeActivity(String userId, String activityId) {
        SocialActivity activity = socialActivityMapper.selectById(activityId);
        if (activity != null) {
            activity.setLikeCount(activity.getLikeCount() + 1);
            socialActivityMapper.updateById(activity);
        }
    }

    @Override
    public void unlikeActivity(String userId, String activityId) {
        SocialActivity activity = socialActivityMapper.selectById(activityId);
        if (activity != null && activity.getLikeCount() > 0) {
            activity.setLikeCount(activity.getLikeCount() - 1);
            socialActivityMapper.updateById(activity);
        }
    }

    private void fillUserInfo(List<SocialActivity> activities) {
        if (activities == null || activities.isEmpty()) {
            return;
        }

        Set<String> userIds = activities.stream()
                .map(SocialActivity::getUserId)
                .collect(Collectors.toSet());

        if (userIds.isEmpty()) {
            return;
        }

        List<User> users = userMapper.selectBatchIds(userIds);
        Map<String, User> userMap = users.stream()
                .collect(Collectors.toMap(User::getUserId, u -> u));

        for (SocialActivity activity : activities) {
            User user = userMap.get(activity.getUserId());
            if (user != null) {
                SocialActivity.UserInfo userInfo = new SocialActivity.UserInfo();
                userInfo.setUserId(user.getUserId());
                userInfo.setUsername(user.getUsername());
                userInfo.setAvatarUrl(user.getAvatarUrl());
                activity.setUserInfo(userInfo);
            }
        }
    }
}