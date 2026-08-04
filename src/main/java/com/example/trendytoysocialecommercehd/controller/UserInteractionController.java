package com.example.trendytoysocialecommercehd.controller;

import com.example.trendytoysocialecommercehd.common.Result;
import com.example.trendytoysocialecommercehd.entity.SocialActivity;
import com.example.trendytoysocialecommercehd.entity.User;
import com.example.trendytoysocialecommercehd.entity.Product;
import com.example.trendytoysocialecommercehd.mapper.CommentMapper;
import com.example.trendytoysocialecommercehd.mapper.ProductMapper;
import com.example.trendytoysocialecommercehd.mapper.SaleVariantMapper;
import com.example.trendytoysocialecommercehd.mapper.SaleSeriesMapper;
import com.example.trendytoysocialecommercehd.mapper.ShopMapper;
import com.example.trendytoysocialecommercehd.mapper.SocialActivityMapper;
import com.example.trendytoysocialecommercehd.mapper.UserInteractionMapper;
import com.example.trendytoysocialecommercehd.service.SocialActivityService;
import com.example.trendytoysocialecommercehd.service.UserInteractionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/interaction")
public class UserInteractionController {

    @Autowired
    private UserInteractionService interactionService;

    @Autowired
    private SocialActivityService socialActivityService;

    @Autowired
    private UserInteractionMapper userInteractionMapper;

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private SocialActivityMapper socialActivityMapper;

    @Autowired
    private SaleVariantMapper saleVariantMapper;

    @Autowired
    private SaleSeriesMapper saleSeriesMapper;

    @Autowired
    private ShopMapper shopMapper;

    @Autowired
    private ProductMapper productMapper;

    // 点赞/取消点赞
    @PostMapping("/like")
    public Result<?> toggleLike(@RequestBody Map<String, String> body) {
        String userId = getCurrentUserId();
        String targetType = body.get("targetType");
        String targetId = body.get("targetId");
        boolean liked = interactionService.toggleLike(userId, targetType, targetId);
        Map<String, Object> result = new HashMap<>();
        result.put("liked", liked);
        result.put("likeCount", interactionService.getLikeCount(targetType, targetId));
        return Result.success(result);
    }

    // 检查是否已点赞
    @GetMapping("/like/check")
    public Result<?> checkLiked(@RequestParam String targetType, @RequestParam String targetId) {
        String userId = getCurrentUserId();
        boolean liked = interactionService.isLiked(userId, targetType, targetId);
        Map<String, Object> result = new HashMap<>();
        result.put("liked", liked);
        result.put("likeCount", interactionService.getLikeCount(targetType, targetId));
        return Result.success(result);
    }

    // 收藏/取消收藏
    @PostMapping("/favorite")
    public Result<?> toggleFavorite(@RequestBody Map<String, String> body) {
        String userId = getCurrentUserId();
        String targetType = body.get("targetType");
        String targetId = body.get("targetId");
        boolean favorited = interactionService.toggleFavorite(userId, targetType, targetId);
        Map<String, Object> result = new HashMap<>();
        result.put("favorited", favorited);
        result.put("favoriteCount", interactionService.getFavoriteCount(targetType, targetId));
        return Result.success(result);
    }

    // 检查是否已收藏
    @GetMapping("/favorite/check")
    public Result<?> checkFavorited(@RequestParam String targetType, @RequestParam String targetId) {
        String userId = getCurrentUserId();
        boolean favorited = interactionService.isFavorited(userId, targetType, targetId);
        Map<String, Object> result = new HashMap<>();
        result.put("favorited", favorited);
        result.put("favoriteCount", interactionService.getFavoriteCount(targetType, targetId));
        return Result.success(result);
    }

    // 关注/取消关注
    @PostMapping("/follow")
    public Result<?> toggleFollow(@RequestBody Map<String, String> body) {
        String followerId = getCurrentUserId();
        String followingId = body.get("targetId");
        boolean following = interactionService.toggleFollow(followerId, followingId);
        Map<String, Object> result = new HashMap<>();
        result.put("following", following);
        result.put("followerCount", interactionService.getFollowerCount(followingId));
        return Result.success(result);
    }

    // 检查是否已关注
    @GetMapping("/follow/check")
    public Result<?> checkFollowing(@RequestParam String targetId) {
        String userId = getCurrentUserId();
        boolean following = interactionService.isFollowing(userId, targetId);
        Map<String, Object> result = new HashMap<>();
        result.put("following", following);
        return Result.success(result);
    }

    // 获取关注列表
    @GetMapping("/following")
    public Result<?> getFollowingList() {
        String userId = getCurrentUserId();
        List<User> followingList = interactionService.getFollowingList(userId);
        List<Map<String, Object>> result = followingList.stream().map(user -> {
            Map<String, Object> map = new HashMap<>();
            map.put("userId", user.getUserId());
            map.put("username", user.getUsername());
            map.put("avatarUrl", user.getAvatarUrl());
            map.put("followerCount", interactionService.getFollowerCount(user.getUserId()));
            return map;
        }).collect(Collectors.toList());
        return Result.success(result);
    }

    // 获取粉丝列表
    @GetMapping("/followers")
    public Result<?> getFollowerList() {
        String userId = getCurrentUserId();
        List<User> followerList = interactionService.getFollowerList(userId);
        List<Map<String, Object>> result = followerList.stream().map(user -> {
            Map<String, Object> map = new HashMap<>();
            map.put("userId", user.getUserId());
            map.put("username", user.getUsername());
            map.put("avatarUrl", user.getAvatarUrl());
            map.put("followerCount", interactionService.getFollowerCount(user.getUserId()));
            return map;
        }).collect(Collectors.toList());
        return Result.success(result);
    }

    // 获取用户关注/粉丝数
    @GetMapping("/stats")
    public Result<?> getInteractionStats(@RequestParam(required = false) String userId) {
        if (userId == null) {
            userId = getCurrentUserId();
        }
        Map<String, Object> result = new HashMap<>();
        result.put("followingCount", interactionService.getFollowingCount(userId));
        result.put("followerCount", interactionService.getFollowerCount(userId));
        return Result.success(result);
    }

    // 获取某类型的收藏列表
    @GetMapping("/favorites/{targetType}")
    public Result<?> getFavoritesByType(@PathVariable String targetType) {
        String userId = getCurrentUserId();
        List<String> targetIds = interactionService.getFavoriteTargetIds(userId, targetType);
        if ("ACTIVITY".equals(targetType)) {
            return Result.success(buildActivityList(targetIds));
        }
        if ("PRODUCT".equals(targetType)) {
            return Result.success(buildProductList(targetIds));
        }
        return Result.success(targetIds);
    }

    // 获取某类型的点赞列表
    @GetMapping("/likes/{targetType}")
    public Result<?> getLikesByType(@PathVariable String targetType) {
        String userId = getCurrentUserId();
        List<String> targetIds = interactionService.getLikedTargetIds(userId, targetType);
        if ("ACTIVITY".equals(targetType)) {
            return Result.success(buildActivityList(targetIds));
        }
        if ("PRODUCT".equals(targetType)) {
            return Result.success(buildProductList(targetIds));
        }
        return Result.success(targetIds);
    }

    // 构建商品详情列表（同时支持 SaleVariant 和 Product 图鉴款式）
    private java.util.List<Map<String, Object>> buildProductList(List<String> targetIds) {
        java.util.List<Map<String, Object>> products = new java.util.ArrayList<>();
        for (String targetId : targetIds) {
            // 先尝试从 Product（图鉴款式）表查找
            Product product = productMapper.selectById(targetId);
            if (product != null) {
                Map<String, Object> map = new HashMap<>();
                map.put("productId", product.getProductId());
                map.put("productName", product.getName());
                map.put("variantName", product.getName());
                map.put("price", product.getPrice());
                String image = parseVariantImage(product.getImageUrl());
                map.put("productImage", image);
                map.put("variantImage", image);
                // 系列信息
                if (product.getSeriesId() != null) {
                    com.example.trendytoysocialecommercehd.entity.SaleSeries series = 
                        saleSeriesMapper.selectById(product.getSeriesId());
                    if (series != null && series.getShopId() != null) {
                        com.example.trendytoysocialecommercehd.entity.Shop shop = 
                            shopMapper.selectById(series.getShopId());
                        if (shop != null) {
                            map.put("shopId", series.getShopId());
                            map.put("shopName", shop.getShopName());
                        }
                    }
                }
                products.add(map);
                continue;
            }
            // 如果 Product 表没有，再尝试从 SaleVariant 表查找
            com.example.trendytoysocialecommercehd.entity.SaleVariant variant = 
                saleVariantMapper.selectById(targetId);
            if (variant != null) {
                Map<String, Object> map = new HashMap<>();
                map.put("saleVariantId", variant.getSaleVariantId());
                map.put("productId", variant.getSaleVariantId());
                map.put("productName", variant.getCustomDescription() != null 
                    ? variant.getCustomDescription() : variant.getSkuCode());
                map.put("variantName", variant.getCustomDescription() != null 
                    ? variant.getCustomDescription() : variant.getSkuCode());
                map.put("price", variant.getSalePrice());
                // 解析图片
                String image = parseVariantImage(variant.getCustomImages());
                map.put("productImage", image);
                map.put("variantImage", image);
                // 获取店铺信息
                if (variant.getSaleSeriesId() != null) {
                    com.example.trendytoysocialecommercehd.entity.SaleSeries series = 
                        saleSeriesMapper.selectById(variant.getSaleSeriesId());
                    if (series != null) {
                        map.put("shopId", series.getShopId());
                        if (series.getShopId() != null) {
                            com.example.trendytoysocialecommercehd.entity.Shop shop = 
                                shopMapper.selectById(series.getShopId());
                            if (shop != null) {
                                map.put("shopName", shop.getShopName());
                            }
                        }
                    }
                }
                products.add(map);
            }
        }
        return products;
    }

    private String parseVariantImage(String customImages) {
        if (customImages == null || customImages.isEmpty()) return "";
        try {
            if (customImages.startsWith("[") && customImages.endsWith("]")) {
                String parsed = customImages.substring(1, customImages.length() - 1);
                if (parsed.contains(",")) {
                    return parsed.split(",")[0].trim().replaceAll("\"", "");
                }
                return parsed.trim().replaceAll("\"", "");
            }
            return customImages;
        } catch (Exception e) {
            return customImages;
        }
    }

    // 构建动态详情列表（公共方法）
    private java.util.List<Map<String, Object>> buildActivityList(List<String> activityIds) {
        java.util.List<Map<String, Object>> activities = new java.util.ArrayList<>();
        for (String activityId : activityIds) {
            SocialActivity activity = socialActivityService.getActivityById(activityId);
            if (activity != null) {
                Map<String, Object> map = new HashMap<>();
                map.put("activityId", activity.getActivityId());
                map.put("title", activity.getTitle());
                map.put("coverImage", activity.getCoverImage());
                map.put("likeCount", activity.getRealLikeCount() != null ? activity.getRealLikeCount() : activity.getLikeCount());
                map.put("viewCount", activity.getRealViewCount() != null ? activity.getRealViewCount() : activity.getViewCount());
                if (activity.getUserInfo() != null) {
                    Map<String, Object> userInfoMap = new HashMap<>();
                    userInfoMap.put("userId", activity.getUserInfo().getUserId());
                    userInfoMap.put("username", activity.getUserInfo().getUsername());
                    userInfoMap.put("avatarUrl", activity.getUserInfo().getAvatarUrl());
                    map.put("userInfo", userInfoMap);
                }
                activities.add(map);
            }
        }
        return activities;
    }

    // 获取当前用户所有动态ID
    private List<String> getCurrentUserActivityIds() {
        String userId = getCurrentUserId();
        List<SocialActivity> activities = socialActivityMapper.selectByUserId(userId);
        return activities.stream().map(SocialActivity::getActivityId).collect(Collectors.toList());
    }

    // 通知：点赞通知
    @GetMapping("/notifications/likes")
    public Result<?> getLikeNotifications() {
        String currentUserId = getCurrentUserId();
        List<String> activityIds = getCurrentUserActivityIds();
        if (activityIds.isEmpty()) {
            return Result.success(new ArrayList<>());
        }
        List<Map<String, Object>> notifications = userInteractionMapper.findLikeNotifications(currentUserId, activityIds);
        return Result.success(notifications);
    }

    // 通知：关注通知
    @GetMapping("/notifications/follows")
    public Result<?> getFollowNotifications() {
        String currentUserId = getCurrentUserId();
        List<Map<String, Object>> notifications = userInteractionMapper.findFollowNotifications(currentUserId);
        return Result.success(notifications);
    }

    // 通知：评论通知
    @GetMapping("/notifications/comments")
    public Result<?> getCommentNotifications() {
        List<String> activityIds = getCurrentUserActivityIds();
        if (activityIds.isEmpty()) {
            return Result.success(new ArrayList<>());
        }
        List<Map<String, Object>> notifications = commentMapper.findCommentNotifications(activityIds);
        return Result.success(notifications);
    }

    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }
}
