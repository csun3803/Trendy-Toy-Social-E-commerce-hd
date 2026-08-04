package com.example.trendytoysocialecommercehd.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.trendytoysocialecommercehd.entity.Report;
import com.example.trendytoysocialecommercehd.entity.SocialActivity;
import com.example.trendytoysocialecommercehd.entity.User;
import com.example.trendytoysocialecommercehd.mapper.ReportMapper;
import com.example.trendytoysocialecommercehd.mapper.SocialActivityMapper;
import com.example.trendytoysocialecommercehd.mapper.UserMapper;
import com.example.trendytoysocialecommercehd.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private ReportMapper reportMapper;

    @Autowired
    private SocialActivityMapper socialActivityMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    @Transactional
    public Report createReport(String reporterId, String targetType, String targetId, String reason) {
        // 防重复举报
        LambdaQueryWrapper<Report> existWrapper = new LambdaQueryWrapper<>();
        existWrapper.eq(Report::getReporterId, reporterId)
                    .eq(Report::getTargetType, targetType)
                    .eq(Report::getTargetId, targetId);
        if (reportMapper.selectCount(existWrapper) > 0) {
            throw new RuntimeException("您已举报过该内容");
        }

        Report report = new Report();
        report.setReporterId(reporterId);
        report.setTargetType(targetType);
        report.setTargetId(targetId);
        report.setReason(reason);
        report.setStatus("PENDING");
        reportMapper.insert(report);

        // 更新关联帖子：举报次数+1，标记有待处理举报
        if ("ACTIVITY".equals(targetType)) {
            SocialActivity activity = socialActivityMapper.selectById(targetId);
            if (activity != null) {
                activity.setReportCount((activity.getReportCount() != null ? activity.getReportCount() : 0) + 1);
                activity.setHasPendingReport(true);
                // 举报不影响审核状态，帖子仍保持原审核状态
                socialActivityMapper.updateById(activity);
            }
        }

        fillReporterInfo(Collections.singletonList(report));
        return report;
    }

    @Override
    public Page<Report> getReports(int page, int size, String status, String targetType) {
        Page<Report> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<Report> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Report::getStatus, status);
        }
        if (targetType != null && !targetType.isEmpty()) {
            wrapper.eq(Report::getTargetType, targetType);
        }
        wrapper.orderByDesc(Report::getCreatedAt);

        Page<Report> result = reportMapper.selectPage(pageObj, wrapper);
        fillReporterInfo(result.getRecords());
        return result;
    }

    @Override
    @Transactional
    public void resolveReport(String reportId, String resolverId, String status, String resolveNotes) {
        Report report = reportMapper.selectById(reportId);
        if (report == null) {
            throw new RuntimeException("举报记录不存在");
        }
        report.setStatus(status);
        report.setResolvedBy(resolverId);
        report.setResolvedAt(new Date());
        report.setResolveNotes(resolveNotes);
        reportMapper.updateById(report);

        // 检查该目标是否还有未处理的举报
        if ("ACTIVITY".equals(report.getTargetType())) {
            int pendingCount = reportMapper.countPendingReports(report.getTargetType(), report.getTargetId());
            if (pendingCount == 0) {
                SocialActivity activity = socialActivityMapper.selectById(report.getTargetId());
                if (activity != null) {
                    activity.setHasPendingReport(false);
                    socialActivityMapper.updateById(activity);
                }
            }
        }
    }

    @Override
    public boolean hasReported(String reporterId, String targetType, String targetId) {
        LambdaQueryWrapper<Report> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Report::getReporterId, reporterId)
               .eq(Report::getTargetType, targetType)
               .eq(Report::getTargetId, targetId);
        return reportMapper.selectCount(wrapper) > 0;
    }

    private void fillReporterInfo(List<Report> reports) {
        if (reports == null || reports.isEmpty()) return;
        Set<String> userIds = reports.stream()
                .map(Report::getReporterId)
                .collect(Collectors.toSet());
        if (userIds.isEmpty()) return;

        List<User> users = userMapper.selectBatchIds(userIds);
        Map<String, User> userMap = users.stream()
                .collect(Collectors.toMap(User::getUserId, u -> u));

        for (Report report : reports) {
            User user = userMap.get(report.getReporterId());
            if (user != null) {
                Report.ReporterInfo info = new Report.ReporterInfo();
                info.setUserId(user.getUserId());
                info.setUsername(user.getUsername());
                info.setAvatarUrl(user.getAvatarUrl());
                report.setReporterInfo(info);
            }
        }
    }
}
