package com.example.trendytoysocialecommercehd.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.trendytoysocialecommercehd.common.Result;
import com.example.trendytoysocialecommercehd.entity.ActivityProductReference;
import com.example.trendytoysocialecommercehd.service.ActivityProductReferenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class ActivityProductReferenceController {

    @Autowired
    private ActivityProductReferenceService referenceService;

    @GetMapping("/activity/{activityId}/series")
    public Result<IPage<?>> getSeriesByActivity(
            @PathVariable String activityId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size
    ) {
        Page<ActivityProductReference> pageParam = new Page<>(page, size);
        IPage<ActivityProductReference> resultPage = referenceService.getSeriesByActivity(pageParam, activityId);

        List<Object> seriesList = resultPage.getRecords().stream()
                .map(ActivityProductReference::getSeries)
                .filter(s -> s != null)
                .map(s -> (Object) s)
                .collect(Collectors.toList());

        Page<Object> result = new Page<>(page, size, resultPage.getTotal());
        result.setRecords(seriesList);
        return Result.success(result);
    }

    @GetMapping("/series/{seriesId}/activities")
    public Result<IPage<?>> getActivitiesBySeries(
            @PathVariable String seriesId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size
    ) {
        Page<ActivityProductReference> pageParam = new Page<>(page, size);
        IPage<ActivityProductReference> resultPage = referenceService.getActivitiesBySeries(pageParam, seriesId);

        List<Object> activitiesList = resultPage.getRecords().stream()
                .map(ActivityProductReference::getActivity)
                .filter(a -> a != null)
                .map(a -> (Object) a)
                .collect(Collectors.toList());

        Page<Object> result = new Page<>(page, size, resultPage.getTotal());
        result.setRecords(activitiesList);
        return Result.success(result);
    }

    @PostMapping("/activity/series/reference")
    public Result<ActivityProductReference> addReference(@RequestBody Map<String, String> params) {
        String activityId = params.get("activityId");
        String seriesId = params.get("seriesId");
        ActivityProductReference reference = referenceService.addReference(activityId, seriesId);
        return Result.success(reference);
    }

    @DeleteMapping("/activity/series/reference/{referenceId}")
    public Result<Void> removeReference(@PathVariable String referenceId) {
        referenceService.removeReference(referenceId);
        return Result.success();
    }
}