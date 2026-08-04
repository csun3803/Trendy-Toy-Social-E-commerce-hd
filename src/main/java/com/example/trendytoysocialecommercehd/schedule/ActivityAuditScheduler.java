package com.example.trendytoysocialecommercehd.schedule;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.trendytoysocialecommercehd.entity.SocialActivity;
import com.example.trendytoysocialecommercehd.mapper.SocialActivityMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 * 社区动态自动审核定时任务
 * 每分钟检查一次，将发布超过30分钟且未被处理、无待处理举报的帖子自动标记为审核通过
 */
@Slf4j
@Component
public class ActivityAuditScheduler {

    @Autowired
    private SocialActivityMapper socialActivityMapper;

    @Scheduled(fixedRate = 60000)
    public void autoApproveActivities() {
        // 计算30分钟前的时间阈值
        Date threshold = new Date(System.currentTimeMillis() - 30 * 60 * 1000);

        LambdaQueryWrapper<SocialActivity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SocialActivity::getAuditStatus, "待审核")
               .eq(SocialActivity::getPublishStatus, "published")
               .eq(SocialActivity::getHasPendingReport, false)
               .le(SocialActivity::getPublishedAt, threshold);

        List<SocialActivity> activities = socialActivityMapper.selectList(wrapper);

        if (!activities.isEmpty()) {
            log.info("自动审核：发现 {} 条待审核帖子已超过30分钟，自动通过", activities.size());
            for (SocialActivity activity : activities) {
                activity.setAuditStatus("审核通过");
                activity.setAuditorId("SYSTEM");
                activity.setAuditedAt(new Date());
                activity.setAuditNotes("系统自动审核通过");
                socialActivityMapper.updateById(activity);
            }
        }
    }
}
