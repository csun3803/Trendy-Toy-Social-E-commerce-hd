package com.example.trendytoysocialecommercehd.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.trendytoysocialecommercehd.common.Result;
import com.example.trendytoysocialecommercehd.entity.SocialActivity;
import com.example.trendytoysocialecommercehd.service.SocialActivityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/activity")
public class SocialActivityController {

    @Autowired
    private SocialActivityService socialActivityService;

    @GetMapping
    public Result<?> getActivityList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String activityType) {
        Page<SocialActivity> result = socialActivityService.getPublicActivities(page, size, activityType);
        return Result.success(result);
    }

    @GetMapping("/my")
    public Result<?> getMyActivities(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String publishStatus) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();
        Page<SocialActivity> result = socialActivityService.getMyActivities(userId, page, size, publishStatus);
        return Result.success(result);
    }

    @GetMapping("/{id}")
    public Result<?> getActivityById(@PathVariable String id) {
        SocialActivity activity = socialActivityService.getActivityById(id);
        if (activity == null) {
            return Result.error("动态不存在");
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
}