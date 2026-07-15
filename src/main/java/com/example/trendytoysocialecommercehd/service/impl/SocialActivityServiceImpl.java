package com.example.trendytoysocialecommercehd.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.trendytoysocialecommercehd.entity.SocialActivity;
import com.example.trendytoysocialecommercehd.entity.User;
import com.example.trendytoysocialecommercehd.mapper.SocialActivityMapper;
import com.example.trendytoysocialecommercehd.mapper.UserMapper;
import com.example.trendytoysocialecommercehd.service.SocialActivityService;
import com.example.trendytoysocialecommercehd.service.UserInteractionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.*;
import java.util.stream.Collectors;

@Service
public class SocialActivityServiceImpl implements SocialActivityService {

    @Autowired
    private SocialActivityMapper socialActivityMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserInteractionService userInteractionService;

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
    public Page<SocialActivity> getMyActivities(String userId, int page, int size, String publishStatus, String activityType) {
        Page<SocialActivity> pageObj = new Page<>(page, size);
        QueryWrapper<SocialActivity> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        if (publishStatus != null && !publishStatus.isEmpty()) {
            wrapper.eq("publish_status", publishStatus);
        }
        if (activityType != null && !activityType.isEmpty()) {
            wrapper.eq("activity_type", activityType);
        }
        wrapper.orderByDesc("updated_at");

        Page<SocialActivity> result = socialActivityMapper.selectPage(pageObj, wrapper);
        fillUserInfo(result.getRecords());  // 如果已经添加了 fillUserInfo 方法
        return result;
    }

    @Override
    public SocialActivity getActivityById(String activityId) {
        SocialActivity activity = socialActivityMapper.selectById(activityId);
        if (activity != null) {
            // 获取真实统计数据
            activity.setRealViewCount(userInteractionService.getViewCount("ACTIVITY", activityId));
            activity.setRealLikeCount(userInteractionService.getLikeCount("ACTIVITY", activityId));
            activity.setRealFavoriteCount(userInteractionService.getFavoriteCount("ACTIVITY", activityId));
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
        activity.setUpdatedAt(new Date());

        if ("published".equals(activity.getPublishStatus())) {
            activity.setAuditStatus("待审核");
            activity.setPublishedAt(new Date());
        } else {
            activity.setPublishStatus("draft");
            activity.setAuditStatus(null);
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

        if ("published".equals(activity.getPublishStatus())) {
            if (existing.getPublishedAt() == null) {
                activity.setPublishedAt(new Date());
            }
            if ("审核拒绝".equals(existing.getAuditStatus()) || "draft".equals(existing.getPublishStatus())) {
                activity.setAuditStatus("待审核");
                activity.setAuditNotes(null);
                activity.setAuditorId(null);
                activity.setAuditedAt(null);
            }
        } else {
            activity.setPublishStatus("draft");
            activity.setAuditStatus(null);
            activity.setAuditNotes(null);
            activity.setAuditorId(null);
            activity.setAuditedAt(null);
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
        userInteractionService.toggleLike(userId, "ACTIVITY", activityId);
    }

    @Override
    public void unlikeActivity(String userId, String activityId) {
        userInteractionService.toggleLike(userId, "ACTIVITY", activityId);
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

    @Override
    public Page<SocialActivity> getAdminActivities(int page, int size, String auditStatus, String publishStatus, String activityType) {
        Page<SocialActivity> pageObj = new Page<>(page, size);
        QueryWrapper<SocialActivity> wrapper = new QueryWrapper<>();

        if (auditStatus != null && !auditStatus.isEmpty()) {
            wrapper.eq("audit_status", auditStatus);
        }
        if (publishStatus != null && !publishStatus.isEmpty()) {
            wrapper.eq("publish_status", publishStatus);
        }
        if (activityType != null && !activityType.isEmpty()) {
            wrapper.eq("activity_type", activityType);
        }
        wrapper.orderByDesc("updated_at");

        Page<SocialActivity> result = socialActivityMapper.selectPage(pageObj, wrapper);
        fillUserInfo(result.getRecords());
        return result;
    }

    @Override
    public void auditActivity(String activityId, String auditorId, String auditStatus, String auditNotes) {
        SocialActivity activity = socialActivityMapper.selectById(activityId);
        if (activity == null) {
            throw new RuntimeException("动态不存在");
        }

        if ("审核拒绝".equals(auditStatus) && (auditNotes == null || auditNotes.trim().isEmpty())) {
            throw new RuntimeException("审核拒绝时必须填写拒绝原因");
        }

        activity.setAuditStatus(auditStatus);
        activity.setAuditNotes(auditNotes);
        activity.setAuditorId(auditorId);
        activity.setAuditedAt(new Date());

        if ("审核通过".equals(auditStatus)) {
            activity.setPublishStatus("published");
            if (activity.getPublishedAt() == null) {
                activity.setPublishedAt(new Date());
            }
        }

        socialActivityMapper.updateById(activity);
    }

    @Override
    public void adminDeleteActivity(String activityId) {
        SocialActivity activity = socialActivityMapper.selectById(activityId);
        if (activity == null) {
            throw new RuntimeException("动态不存在");
        }
        socialActivityMapper.deleteById(activityId);
    }

    @Override
    public Map<String, Long> getActivityStats() {
        Map<String, Long> stats = new HashMap<>();

        // 总数
        long total = socialActivityMapper.selectCount(null);
        stats.put("total", total);

        // 待审核
        long pending = socialActivityMapper.selectCount(new QueryWrapper<SocialActivity>()
                .eq("audit_status", "待审核"));
        stats.put("pending", pending);

        // 已通过
        long approved = socialActivityMapper.selectCount(new QueryWrapper<SocialActivity>()
                .eq("audit_status", "审核通过"));
        stats.put("approved", approved);

        // 已拒绝
        long rejected = socialActivityMapper.selectCount(new QueryWrapper<SocialActivity>()
                .eq("audit_status", "审核拒绝"));
        stats.put("rejected", rejected);

        // 已发布
        long published = socialActivityMapper.selectCount(new QueryWrapper<SocialActivity>()
                .eq("publish_status", "published")
                .eq("audit_status", "审核通过"));
        stats.put("published", published);

        return stats;
    }

    @Override
    @Transactional
    public void incrementCommentCount(String activityId) {
        SocialActivity activity = socialActivityMapper.selectById(activityId);
        if (activity != null) {
            activity.setCommentCount(activity.getCommentCount() + 1);
            socialActivityMapper.updateById(activity);
        }
    }
}