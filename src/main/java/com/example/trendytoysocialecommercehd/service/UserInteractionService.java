package com.example.trendytoysocialecommercehd.service;

import com.example.trendytoysocialecommercehd.entity.User;
import java.util.List;
import java.util.Map;

public interface UserInteractionService {
    // 点赞/取消点赞
    boolean toggleLike(String userId, String targetType, String targetId);
    boolean isLiked(String userId, String targetType, String targetId);
    int getLikeCount(String targetType, String targetId);

    // 收藏/取消收藏
    boolean toggleFavorite(String userId, String targetType, String targetId);
    boolean isFavorited(String userId, String targetType, String targetId);
    int getFavoriteCount(String targetType, String targetId);

    // 关注/取消关注
    boolean toggleFollow(String followerId, String followingId);
    boolean isFollowing(String followerId, String followingId);
    int getFollowingCount(String userId);
    int getFollowerCount(String userId);

    // 浏览记录
    void recordView(String userId, String targetType, String targetId);
    int getViewCount(String targetType, String targetId);

    // 获取关注列表
    List<User> getFollowingList(String userId);
    // 获取粉丝列表
    List<User> getFollowerList(String userId);

    // 获取某类型的收藏列表
    List<String> getFavoriteTargetIds(String userId, String targetType);

    // 获取某类型的点赞列表
    List<String> getLikedTargetIds(String userId, String targetType);
}
