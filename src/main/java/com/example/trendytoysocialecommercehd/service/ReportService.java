package com.example.trendytoysocialecommercehd.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.trendytoysocialecommercehd.entity.Report;

public interface ReportService {
    Report createReport(String reporterId, String targetType, String targetId, String reason);
    Page<Report> getReports(int page, int size, String status, String targetType);
    void resolveReport(String reportId, String resolverId, String status, String resolveNotes);
    boolean hasReported(String reporterId, String targetType, String targetId);
}
