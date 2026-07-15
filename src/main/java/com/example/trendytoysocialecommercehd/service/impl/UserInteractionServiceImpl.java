package com.example.trendytoysocialecommercehd.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.trendytoysocialecommercehd.entity.User;
import com.example.trendytoysocialecommercehd.entity.UserInteraction;
import com.example.trendytoysocialecommercehd.mapper.UserInteractionMapper;
import com.example.trendytoysocialecommercehd.mapper.UserMapper;
import com.example.trendytoysocialecommercehd.service.UserInteractionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserInteractionServiceImpl implements UserInteractionService {

    @Autowired
    private UserInteractionMapper userInteractionMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    public boolean toggleLike(String userId, String targetType, String targetId) {
        UserInteraction existing = userInteractionMapper.findByUserAndTarget(userId, targetType, targetId, "LIKE");
        if (existing != null) {
            String newStatus = "ACTIVE".equals(existing.getStatus()) ? "CANCELLED" : "ACTIVE";
            existing.setStatus(newStatus);
            userInteractionMapper.updateById(existing);
            return "ACTIVE".equals(newStatus);
        } else {
            UserInteraction interaction = new UserInteraction();
            interaction.setInteractionId(UUID.randomUUID().toString());
            interaction.setUserId(userId);
            interaction.setTargetType(targetType);
            interaction.setTargetId(targetId);
            interaction.setActionType("LIKE");
            interaction.setStatus("ACTIVE");
            interaction.setCreatedAt(new Date());
            interaction.setUpdatedAt(new Date());
            userInteractionMapper.insert(interaction);
            return true;
        }
    }

    @Override
    public boolean isLiked(String userId, String targetType, String targetId) {
        UserInteraction existing = userInteractionMapper.findByUserAndTarget(userId, targetType, targetId, "LIKE");
        return existing != null && "ACTIVE".equals(existing.getStatus());
    }

    @Override
    public int getLikeCount(String targetType, String targetId) {
        return userInteractionMapper.countActiveInteractions(targetType, targetId, "LIKE");
    }

    @Override
    public boolean toggleFavorite(String userId, String targetType, String targetId) {
        UserInteraction existing = userInteractionMapper.findByUserAndTarget(userId, targetType, targetId, "FAVORITE");
        if (existing != null) {
            String newStatus = "ACTIVE".equals(existing.getStatus()) ? "CANCELLED" : "ACTIVE";
            existing.setStatus(newStatus);
            userInteractionMapper.updateById(existing);
            return "ACTIVE".equals(newStatus);
        } else {
            UserInteraction interaction = new UserInteraction();
            interaction.setInteractionId(UUID.randomUUID().toString());
            interaction.setUserId(userId);
            interaction.setTargetType(targetType);
            interaction.setTargetId(targetId);
            interaction.setActionType("FAVORITE");
            interaction.setStatus("ACTIVE");
            interaction.setCreatedAt(new Date());
            interaction.setUpdatedAt(new Date());
            userInteractionMapper.insert(interaction);
            return true;
        }
    }

    @Override
    public boolean isFavorited(String userId, String targetType, String targetId) {
        UserInteraction existing = userInteractionMapper.findByUserAndTarget(userId, targetType, targetId, "FAVORITE");
        return existing != null && "ACTIVE".equals(existing.getStatus());
    }

    @Override
    public int getFavoriteCount(String targetType, String targetId) {
        return userInteractionMapper.countActiveInteractions(targetType, targetId, "FAVORITE");
    }

    @Override
    public boolean toggleFollow(String followerId, String followingId) {
        if (followerId.equals(followingId)) {
            throw new RuntimeException("不能关注自己");
        }
        UserInteraction existing = userInteractionMapper.findByUserAndTarget(followerId, "USER", followingId, "FOLLOW");
        if (existing != null) {
            String newStatus = "ACTIVE".equals(existing.getStatus()) ? "CANCELLED" : "ACTIVE";
            existing.setStatus(newStatus);
            userInteractionMapper.updateById(existing);
            return "ACTIVE".equals(newStatus);
        } else {
            UserInteraction interaction = new UserInteraction();
            interaction.setInteractionId(UUID.randomUUID().toString());
            interaction.setUserId(followerId);
            interaction.setTargetType("USER");
            interaction.setTargetId(followingId);
            interaction.setActionType("FOLLOW");
            interaction.setStatus("ACTIVE");
            interaction.setCreatedAt(new Date());
            interaction.setUpdatedAt(new Date());
            userInteractionMapper.insert(interaction);
            return true;
        }
    }

    @Override
    public boolean isFollowing(String followerId, String followingId) {
        UserInteraction existing = userInteractionMapper.findByUserAndTarget(followerId, "USER", followingId, "FOLLOW");
        return existing != null && "ACTIVE".equals(existing.getStatus());
    }

    @Override
    public int getFollowingCount(String userId) {
        return userInteractionMapper.findFollowingIds(userId).size();
    }

    @Override
    public int getFollowerCount(String userId) {
        return userInteractionMapper.findFollowerIds(userId).size();
    }

    @Override
    public void recordView(String userId, String targetType, String targetId) {
        if (userId == null) {
            return; // 未登录用户不记录浏览
        }
        // 检查是否已存在记录，避免唯一索引冲突
        UserInteraction existing = userInteractionMapper.findByUserAndTarget(userId, targetType, targetId, "VIEW");
        if (existing != null) {
            return;
        }
        UserInteraction interaction = new UserInteraction();
        interaction.setInteractionId(UUID.randomUUID().toString());
        interaction.setUserId(userId);
        interaction.setTargetType(targetType);
        interaction.setTargetId(targetId);
        interaction.setActionType("VIEW");
        interaction.setStatus("ACTIVE");
        interaction.setCreatedAt(new Date());
        interaction.setUpdatedAt(new Date());
        userInteractionMapper.insert(interaction);
    }

    @Override
    public int getViewCount(String targetType, String targetId) {
        return userInteractionMapper.countActiveInteractions(targetType, targetId, "VIEW");
    }

    @Override
    public List<User> getFollowingList(String userId) {
        List<String> followingIds = userInteractionMapper.findFollowingIds(userId);
        if (followingIds.isEmpty()) {
            return Collections.emptyList();
        }
        return userMapper.selectBatchIds(followingIds);
    }

    @Override
    public List<User> getFollowerList(String userId) {
        List<String> followerIds = userInteractionMapper.findFollowerIds(userId);
        if (followerIds.isEmpty()) {
            return Collections.emptyList();
        }
        return userMapper.selectBatchIds(followerIds);
    }

    @Override
    public List<String> getFavoriteTargetIds(String userId, String targetType) {
        return userInteractionMapper.findFavoriteTargetIds(userId, targetType);
    }

    @Override
    public List<String> getLikedTargetIds(String userId, String targetType) {
        return userInteractionMapper.findLikedTargetIds(userId, targetType);
    }
}
