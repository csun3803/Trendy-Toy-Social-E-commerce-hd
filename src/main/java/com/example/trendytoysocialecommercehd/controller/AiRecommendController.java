package com.example.trendytoysocialecommercehd.controller;

import com.example.trendytoysocialecommercehd.common.Result;
import com.example.trendytoysocialecommercehd.dto.RecommendDTO;
import com.example.trendytoysocialecommercehd.service.AiRecommendService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai/recommend")
@RequiredArgsConstructor
public class AiRecommendController {

    private final AiRecommendService recommendService;

    /**
     * 个性化推荐（基于用户行为）
     */
    @PostMapping("/personalized")
    public Result<List<Map<String, Object>>> personalizedRecommend(@RequestBody RecommendDTO dto) {
        String userId = dto.getUserId();
        int limit = dto.getLimit() != null ? dto.getLimit() : 10;
        List<Map<String, Object>> result = recommendService.recommendForUser(userId, limit);
        return Result.success(result);
    }

    /**
     * 相似系列推荐
     */
    @GetMapping("/similar/{seriesId}")
    public Result<List<Map<String, Object>>> similarRecommend(
            @PathVariable String seriesId,
            @RequestParam(defaultValue = "6") int limit) {
        List<Map<String, Object>> result = recommendService.recommendSimilar(seriesId, limit);
        return Result.success(result);
    }

    /**
     * 热门推荐（无需登录）
     */
    @GetMapping("/hot")
    public Result<List<Map<String, Object>>> hotRecommend(
            @RequestParam(defaultValue = "10") int limit) {
        List<Map<String, Object>> result = recommendService.hotRecommend(limit);
        return Result.success(result);
    }

    /**
     * 上报用户行为（供AI推荐算法使用）
     * behaviorType: BROWSE / FAVORITE / UNFAVORITE / PURCHASE / SEARCH / SHARE
     * targetType:   SERIES / PRODUCT / SHOP
     */
    @PostMapping("/behavior")
    public Result<Void> reportBehavior(@RequestBody BehaviorRequest req) {
        recommendService.reportBehavior(
                req.getUserId(),
                req.getBehaviorType(),
                req.getTargetType(),
                req.getTargetId(),
                req.getWeight() == null ? 1 : req.getWeight()
        );
        return Result.success();
    }

    @Data
    public static class BehaviorRequest {
        private String userId;
        private String behaviorType;
        private String targetType;
        private String targetId;
        private Integer weight;
    }
}
