package com.example.trendytoysocialecommercehd.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.trendytoysocialecommercehd.config.AiServiceClient;
import com.example.trendytoysocialecommercehd.entity.OrderItem;
import com.example.trendytoysocialecommercehd.entity.Product;
import com.example.trendytoysocialecommercehd.entity.Series;
import com.example.trendytoysocialecommercehd.entity.UserProductFavorite;
import com.example.trendytoysocialecommercehd.mapper.OrderItemMapper;
import com.example.trendytoysocialecommercehd.mapper.ProductMapper;
import com.example.trendytoysocialecommercehd.mapper.SeriesMapper;
import com.example.trendytoysocialecommercehd.mapper.UserProductFavoriteMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 智能推荐服务
 * 优先调用独立 Python ai-service（内容+协同+热度混合算法）
 * Python不可用时，回退到本地简单推荐（同IP/同主题/热度），保证可用性
 *
 * 返回 List<Map<String,Object>> 而非 Series 实体：
 * Python侧返回的JSON字段（seriesId, seriesName, theme, seriesHotness 等）
 * 已经是前端期望的驼峰格式，无需转换
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiRecommendService {

    private final UserProductFavoriteMapper favoriteMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductMapper productMapper;
    private final SeriesMapper seriesMapper;
    private final AiServiceClient aiServiceClient;

    /**
     * 个性化推荐（基于用户行为）
     */
    public List<Map<String, Object>> recommendForUser(String userId, int limit) {
        if (limit <= 0) limit = 10;
        // 1. 优先调用Python ai-service（混合推荐算法）
        try {
            List<Map<String, Object>> result = aiServiceClient.personalizedRecommend(userId, limit);
            if (result != null && !result.isEmpty()) {
                return result;
            }
        } catch (Exception e) {
            log.warn("Python ai-service 个性化推荐失败，回退本地: {}", e.getMessage());
        }
        // 2. 回退本地逻辑
        return localRecommendForUser(userId, limit);
    }

    /**
     * 相似系列推荐
     */
    public List<Map<String, Object>> recommendSimilar(String seriesId, int limit) {
        if (limit <= 0) limit = 6;
        try {
            List<Map<String, Object>> result = aiServiceClient.similarRecommend(seriesId, limit);
            if (result != null && !result.isEmpty()) {
                return result;
            }
        } catch (Exception e) {
            log.warn("Python ai-service 相似推荐失败，回退本地: {}", e.getMessage());
        }
        return localRecommendSimilar(seriesId, limit);
    }

    /**
     * 热门推荐（无需登录）
     */
    public List<Map<String, Object>> hotRecommend(int limit) {
        if (limit <= 0) limit = 10;
        try {
            List<Map<String, Object>> result = aiServiceClient.hotRecommend(limit);
            if (result != null && !result.isEmpty()) {
                return result;
            }
        } catch (Exception e) {
            log.warn("Python ai-service 热门推荐失败，回退本地: {}", e.getMessage());
        }
        return localHotRecommend(limit);
    }

    /**
     * 上报用户行为（供Python推荐算法使用）
     */
    public void reportBehavior(String userId, String behaviorType, String targetType, String targetId, int weight) {
        if (userId == null || userId.isEmpty() || targetId == null || targetId.isEmpty()) return;
        aiServiceClient.reportBehavior(userId, behaviorType, targetType, targetId, weight);
    }

    // ============= 本地回退实现 =============

    private List<Map<String, Object>> localRecommendForUser(String userId, int limit) {
        List<Series> series = localRecommendForUserSeries(userId, limit);
        return series.stream().map(this::seriesToMap).collect(Collectors.toList());
    }

    private List<Map<String, Object>> localRecommendSimilar(String seriesId, int limit) {
        List<Series> series = localRecommendSimilarSeries(seriesId, limit);
        return series.stream().map(this::seriesToMap).collect(Collectors.toList());
    }

    private List<Map<String, Object>> localHotRecommend(int limit) {
        QueryWrapper<Series> wrapper = new QueryWrapper<>();
        wrapper.eq("status", "ON_SALE").orderByDesc("series_hotness").last("LIMIT " + limit);
        return seriesMapper.selectList(wrapper).stream().map(this::seriesToMap).collect(Collectors.toList());
    }

    private List<Series> localRecommendForUserSeries(String userId, int limit) {
        Set<String> userSeriesIds = new HashSet<>();
        // 收藏
        QueryWrapper<UserProductFavorite> favWrapper = new QueryWrapper<>();
        favWrapper.eq("user_id", userId).eq("status", "ACTIVE");
        for (UserProductFavorite fav : favoriteMapper.selectList(favWrapper)) {
            Product p = productMapper.selectById(fav.getProductId());
            if (p != null && p.getSeriesId() != null) userSeriesIds.add(p.getSeriesId());
        }
        // 购买
        QueryWrapper<OrderItem> orderWrapper = new QueryWrapper<>();
        orderWrapper.eq("item_seller_id", userId);
        for (OrderItem it : orderItemMapper.selectList(orderWrapper)) {
            Product p = productMapper.selectById(it.getProductId());
            if (p != null && p.getSeriesId() != null) userSeriesIds.add(p.getSeriesId());
        }

        List<Series> recommended = new ArrayList<>();
        Set<String> exclude = new HashSet<>(userSeriesIds);
        if (!userSeriesIds.isEmpty()) {
            List<Series> userSeries = seriesMapper.selectBatchIds(userSeriesIds);
            Set<String> themes = userSeries.stream().map(Series::getTheme).filter(Objects::nonNull).collect(Collectors.toSet());
            Set<String> ips = userSeries.stream().map(Series::getIpAlbumId).filter(Objects::nonNull).collect(Collectors.toSet());
            if (!ips.isEmpty()) {
                QueryWrapper<Series> w = new QueryWrapper<>();
                w.in("ip_album_id", ips).eq("status", "ON_SALE").orderByDesc("series_hotness").last("LIMIT " + limit);
                for (Series s : seriesMapper.selectList(w)) {
                    if (exclude.add(s.getSeriesId())) recommended.add(s);
                }
            }
            if (recommended.size() < limit && !themes.isEmpty()) {
                QueryWrapper<Series> w = new QueryWrapper<>();
                w.in("theme", themes).eq("status", "ON_SALE").orderByDesc("series_hotness").last("LIMIT " + (limit - recommended.size()));
                for (Series s : seriesMapper.selectList(w)) {
                    if (exclude.add(s.getSeriesId())) recommended.add(s);
                }
            }
        }
        if (recommended.size() < limit) {
            QueryWrapper<Series> w = new QueryWrapper<>();
            w.eq("status", "ON_SALE").orderByDesc("series_hotness").last("LIMIT " + (limit - recommended.size()));
            recommended.addAll(seriesMapper.selectList(w));
        }
        return recommended.stream().limit(limit).collect(Collectors.toList());
    }

    private List<Series> localRecommendSimilarSeries(String seriesId, int limit) {
        Series current = seriesMapper.selectById(seriesId);
        if (current == null) return Collections.emptyList();
        Set<String> exclude = new HashSet<>();
        exclude.add(seriesId);
        List<Series> result = new ArrayList<>();
        if (current.getIpAlbumId() != null) {
            QueryWrapper<Series> w = new QueryWrapper<>();
            w.eq("ip_album_id", current.getIpAlbumId()).ne("series_id", seriesId).eq("status", "ON_SALE").orderByDesc("series_hotness").last("LIMIT " + limit);
            result.addAll(seriesMapper.selectList(w));
            result.forEach(s -> exclude.add(s.getSeriesId()));
        }
        if (result.size() < limit && current.getTheme() != null) {
            QueryWrapper<Series> w = new QueryWrapper<>();
            w.eq("theme", current.getTheme()).eq("status", "ON_SALE").orderByDesc("series_hotness").last("LIMIT " + (limit - result.size()));
            for (Series s : seriesMapper.selectList(w)) {
                if (exclude.add(s.getSeriesId())) result.add(s);
            }
        }
        return result.stream().limit(limit).collect(Collectors.toList());
    }

    /**
     * Series实体 -> Map（驼峰字段，与Python返回格式一致）
     */
    private Map<String, Object> seriesToMap(Series s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("seriesId", s.getSeriesId());
        m.put("seriesName", s.getSeriesName());
        m.put("description", s.getDescription());
        m.put("coverImage", s.getCoverImage());
        m.put("minPrice", s.getMinPrice());
        m.put("fullsetPrice", s.getFullsetPrice());
        m.put("status", s.getStatus());
        m.put("theme", s.getTheme());
        m.put("seriesHotness", s.getSeriesHotness());
        m.put("salesCount", s.getSalesCount());
        m.put("ipAlbumId", s.getIpAlbumId());
        return m;
    }
}
