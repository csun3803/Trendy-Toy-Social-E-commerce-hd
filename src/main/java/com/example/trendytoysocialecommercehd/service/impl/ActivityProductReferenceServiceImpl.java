package com.example.trendytoysocialecommercehd.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.trendytoysocialecommercehd.entity.ActivityProductReference;
import com.example.trendytoysocialecommercehd.entity.Series;
import com.example.trendytoysocialecommercehd.entity.SocialActivity;
import com.example.trendytoysocialecommercehd.mapper.ActivityProductReferenceMapper;
import com.example.trendytoysocialecommercehd.mapper.SocialActivityMapper;
import com.example.trendytoysocialecommercehd.service.ActivityProductReferenceService;
import com.example.trendytoysocialecommercehd.service.SeriesService;
import com.example.trendytoysocialecommercehd.service.SocialActivityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ActivityProductReferenceServiceImpl extends ServiceImpl<ActivityProductReferenceMapper, ActivityProductReference> implements ActivityProductReferenceService {

    @Autowired
    private SeriesService seriesService;

    @Autowired
    private SocialActivityService socialActivityService;

    @Autowired
    private SocialActivityMapper socialActivityMapper;


    @Override
    public IPage<ActivityProductReference> getSeriesByActivity(Page<ActivityProductReference> page, String activityId) {
        // 先查询关联记录
        LambdaQueryWrapper<ActivityProductReference> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ActivityProductReference::getActivityId, activityId);
        IPage<ActivityProductReference> resultPage = this.page(page, wrapper);

        // 为每个关联记录设置Series对象
        List<ActivityProductReference> records = resultPage.getRecords();
        for (ActivityProductReference reference : records) {
            if (reference.getSeriesId() != null) {
                Series series = seriesService.getById(reference.getSeriesId());
                reference.setSeries(series);
            }
        }

        return resultPage;
    }

    @Override
    public IPage<ActivityProductReference> getActivitiesBySeries(Page<ActivityProductReference> page, String seriesId) {
        // 先查询关联记录
        LambdaQueryWrapper<ActivityProductReference> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ActivityProductReference::getSeriesId, seriesId);
        IPage<ActivityProductReference> resultPage = this.page(page, wrapper);

        // 为每个关联记录设置Activity对象
        List<ActivityProductReference> records = resultPage.getRecords();
        for (ActivityProductReference reference : records) {
            if (reference.getActivityId() != null) {
                SocialActivity activity = socialActivityMapper.selectById(reference.getActivityId());
                reference.setActivity(activity);
            }
        }

        return resultPage;
    }

    @Override
    public ActivityProductReference addReference(String activityId, String seriesId) {
        LambdaQueryWrapper<ActivityProductReference> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ActivityProductReference::getActivityId, activityId)
                .eq(ActivityProductReference::getSeriesId, seriesId);
        ActivityProductReference existing = baseMapper.selectOne(wrapper);
        if (existing != null) {
            return existing;
        }
        ActivityProductReference reference = new ActivityProductReference();
        reference.setActivityId(activityId);
        reference.setSeriesId(seriesId);
        baseMapper.insert(reference);
        return reference;
    }

    @Override
    public void removeReference(String referenceId) {
        baseMapper.deleteById(referenceId);
    }
}