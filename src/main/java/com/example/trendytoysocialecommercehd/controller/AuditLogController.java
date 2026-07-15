package com.example.trendytoysocialecommercehd.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.trendytoysocialecommercehd.common.Result;
import com.example.trendytoysocialecommercehd.entity.AuditLog;
import com.example.trendytoysocialecommercehd.service.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/audit-log")
public class AuditLogController {

    @Autowired
    private AuditLogService auditLogService;

    @GetMapping("/list")
    public Result<?> getAuditLogList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String operatorName,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        Page<AuditLog> result = auditLogService.getAuditLogList(page, size, module, action, operatorName, startDate, endDate);
        return Result.success(result);
    }

    @GetMapping("/{logId}")
    public Result<?> getAuditLogById(@PathVariable String logId) {
        AuditLog log = auditLogService.getById(logId);
        if (log == null) {
            return Result.error("日志不存在");
        }
        return Result.success(log);
    }
}
