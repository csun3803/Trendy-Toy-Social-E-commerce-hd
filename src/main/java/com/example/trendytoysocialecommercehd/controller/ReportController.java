package com.example.trendytoysocialecommercehd.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.trendytoysocialecommercehd.common.Result;
import com.example.trendytoysocialecommercehd.entity.Report;
import com.example.trendytoysocialecommercehd.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/report")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @PostMapping
    public Result<?> createReport(@RequestBody Map<String, String> body) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String reporterId = authentication.getName();
        String targetType = body.get("targetType");
        String targetId = body.get("targetId");
        String reason = body.get("reason");
        if (targetType == null || targetId == null || reason == null || reason.trim().isEmpty()) {
            return Result.error("举报类型、目标和原因不能为空");
        }
        try {
            Report report = reportService.createReport(reporterId, targetType, targetId, reason);
            return Result.success(report);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/admin/list")
    public Result<?> getReportList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String targetType) {
        Page<Report> result = reportService.getReports(page, size, status, targetType);
        return Result.success(result);
    }

    @PutMapping("/admin/{id}")
    public Result<?> resolveReport(@PathVariable String id, @RequestBody Map<String, String> body) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String resolverId = authentication.getName();
        String status = body.get("status");
        String resolveNotes = body.get("resolveNotes");
        try {
            reportService.resolveReport(id, resolverId, status, resolveNotes);
            return Result.success("处理成功");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/check")
    public Result<?> checkReported(
            @RequestParam String targetType,
            @RequestParam String targetId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return Result.success(Map.of("reported", false));
        }
        String userId = authentication.getName();
        boolean reported = reportService.hasReported(userId, targetType, targetId);
        return Result.success(Map.of("reported", reported));
    }
}
