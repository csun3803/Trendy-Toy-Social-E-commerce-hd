package com.example.trendytoysocialecommercehd.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.trendytoysocialecommercehd.entity.ActivityProductReference;

public interface ActivityProductReferenceService {

    IPage<ActivityProductReference> getSeriesByActivity(Page<ActivityProductReference> page, String activityId);

    IPage<ActivityProductReference> getActivitiesBySeries(Page<ActivityProductReference> page, String seriesId);

    ActivityProductReference addReference(String activityId, String seriesId);

    void removeReference(String referenceId);
}