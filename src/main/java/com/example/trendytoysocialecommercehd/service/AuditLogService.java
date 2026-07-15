package com.example.trendytoysocialecommercehd.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.trendytoysocialecommercehd.entity.AuditLog;

public interface AuditLogService extends IService<AuditLog> {

    /**
     * 分页查询审计日志
     */
    Page<AuditLog> getAuditLogList(int page, int size, String module, String action,
                                    String operatorName, String startDate, String endDate);

    /**
     * 记录审计日志
     */
    void log(String operatorId, String operatorName, String operatorType,
             String action, String module, String description,
             String targetId, String targetType,
             String method, String requestUrl, String requestParams,
             Integer responseCode, String ipAddress);
}
