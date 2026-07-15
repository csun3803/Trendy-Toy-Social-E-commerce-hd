package com.example.trendytoysocialecommercehd.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.trendytoysocialecommercehd.common.Result;
import com.example.trendytoysocialecommercehd.entity.SocialActivity;
import com.example.trendytoysocialecommercehd.service.SocialActivityService;
import com.example.trendytoysocialecommercehd.service.UserInteractionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/activity")
public class SocialActivityController {

    @Autowired
    private SocialActivityService socialActivityService;

    @Autowired
    private UserInteractionService userInteractionService;

    @GetMapping
    public Result<?> getActivityList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String activityType) {
        Page<SocialActivity> result = socialActivityService.getPublicActivities(page, size, activityType);
        // 注入当前用户的点赞和关注状态
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())) {
            String currentUserId = authentication.getName();
            for (SocialActivity activity : result.getRecords()) {
                activity.setIsLiked(userInteractionService.isLiked(currentUserId, "ACTIVITY", activity.getActivityId()));
                activity.setIsFavorited(userInteractionService.isFavorited(currentUserId, "ACTIVITY", activity.getActivityId()));
                if (activity.getUserId() != null && !activity.getUserId().equals(currentUserId)) {
                    activity.setIsFollowing(userInteractionService.isFollowing(currentUserId, activity.getUserId()));
                } else {
                    activity.setIsFollowing(false);
                }
            }
        }
        return Result.success(result);
    }

    @GetMapping("/my")
    public Result<?> getMyActivities(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String publishStatus,
            @RequestParam(required = false) String activityType) {  // 添加这个参数
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();
        Page<SocialActivity> result = socialActivityService.getMyActivities(userId, page, size, publishStatus, activityType);
        return Result.success(result);
    }

    @GetMapping("/{id}")
    public Result<?> getActivityById(@PathVariable String id) {
        SocialActivity activity = socialActivityService.getActivityById(id);
        if (activity == null) {
            return Result.error("动态不存在");
        }
        // 注入当前用户的点赞和关注状态
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserId = null;
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())) {
            currentUserId = authentication.getName();
            activity.setIsLiked(userInteractionService.isLiked(currentUserId, "ACTIVITY", id));
            activity.setIsFavorited(userInteractionService.isFavorited(currentUserId, "ACTIVITY", id));
            if (activity.getUserId() != null && !activity.getUserId().equals(currentUserId)) {
                activity.setIsFollowing(userInteractionService.isFollowing(currentUserId, activity.getUserId()));
            } else {
                activity.setIsFollowing(false);
            }
        } else {
            activity.setIsLiked(false);
            activity.setIsFavorited(false);
            activity.setIsFollowing(false);
        }
        // 仅已登录用户记录浏览
        if (currentUserId != null) {
            userInteractionService.recordView(currentUserId, "ACTIVITY", id);
        }
        return Result.success(activity);
    }

    @PostMapping
    public Result<?> createActivity(@RequestBody SocialActivity activity) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();
        SocialActivity result = socialActivityService.createActivity(userId, activity);
        return Result.success(result);
    }

    @PutMapping
    public Result<?> updateActivity(@RequestBody SocialActivity activity) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();
        try {
            SocialActivity result = socialActivityService.updateActivity(userId, activity);
            return Result.success(result);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<?> deleteActivity(@PathVariable String id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();
        try {
            socialActivityService.deleteActivity(userId, id);
            return Result.success("删除成功");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/{id}/like")
    public Result<?> likeActivity(@PathVariable String id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();
        socialActivityService.likeActivity(userId, id);
        return Result.success("点赞成功");
    }

    @DeleteMapping("/{id}/like")
    public Result<?> unlikeActivity(@PathVariable String id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();
        socialActivityService.unlikeActivity(userId, id);
        return Result.success("取消点赞成功");
    }

    @GetMapping("/admin/list")
    public Result<?> getAdminActivityList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String auditStatus,
            @RequestParam(required = false) String publishStatus,
            @RequestParam(required = false) String activityType) {
        Page<SocialActivity> result = socialActivityService.getAdminActivities(page, size, auditStatus, publishStatus, activityType);
        return Result.success(result);
    }

    @PutMapping("/admin/audit/{id}")
    public Result<?> auditActivity(@PathVariable String id, @RequestBody Map<String, String> params) {
        String auditStatus = params.get("auditStatus");
        String auditNotes = params.get("auditNotes");
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String auditorId = authentication.getName();
            socialActivityService.auditActivity(id, auditorId, auditStatus, auditNotes);
            return Result.success("审核成功");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/admin/{id}")
    public Result<?> adminDeleteActivity(@PathVariable String id) {
        try {
            socialActivityService.adminDeleteActivity(id);
            return Result.success("删除成功");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/admin/stats")
    public Result<?> getActivityStats() {
        Map<String, Long> stats = socialActivityService.getActivityStats();
        return Result.success(stats);
    }
}