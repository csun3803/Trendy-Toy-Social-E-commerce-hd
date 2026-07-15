package com.example.trendytoysocialecommercehd.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.trendytoysocialecommercehd.entity.AuditLog;
import com.example.trendytoysocialecommercehd.mapper.AuditLogMapper;
import com.example.trendytoysocialecommercehd.service.AuditLogService;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@Service
public class AuditLogServiceImpl extends ServiceImpl<AuditLogMapper, AuditLog> implements AuditLogService {

    @Override
    public Page<AuditLog> getAuditLogList(int page, int size, String module, String action,
                                           String operatorName, String startDate, String endDate) {
        Page<AuditLog> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<>();

        if (module != null && !module.isEmpty()) {
            wrapper.eq(AuditLog::getModule, module);
        }
        if (action != null && !action.isEmpty()) {
            wrapper.eq(AuditLog::getAction, action);
        }
        if (operatorName != null && !operatorName.isEmpty()) {
            wrapper.like(AuditLog::getOperatorName, operatorName);
        }
        if (startDate != null && !startDate.isEmpty()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                Date start = sdf.parse(startDate);
                wrapper.ge(AuditLog::getCreatedAt, start);
            } catch (ParseException e) {
                // ignore
            }
        }
        if (endDate != null && !endDate.isEmpty()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                Date end = sdf.parse(endDate);
                // 结束日期取当天23:59:59
                end = new Date(end.getTime() + 24 * 60 * 60 * 1000 - 1);
                wrapper.le(AuditLog::getCreatedAt, end);
            } catch (ParseException e) {
                // ignore
            }
        }

        wrapper.orderByDesc(AuditLog::getCreatedAt);
        return this.page(pageParam, wrapper);
    }

    @Override
    public void log(String operatorId, String operatorName, String operatorType,
                    String action, String module, String description,
                    String targetId, String targetType,
                    String method, String requestUrl, String requestParams,
                    Integer responseCode, String ipAddress) {
        AuditLog auditLog = new AuditLog();
        auditLog.setOperatorId(operatorId);
        auditLog.setOperatorName(operatorName);
        auditLog.setOperatorType(operatorType);
        auditLog.setAction(action);
        auditLog.setModule(module);
        auditLog.setDescription(description);
        auditLog.setTargetId(targetId);
        auditLog.setTargetType(targetType);
        auditLog.setMethod(method);
        auditLog.setRequestUrl(requestUrl);
        auditLog.setRequestParams(requestParams);
        auditLog.setResponseCode(responseCode);
        auditLog.setIpAddress(ipAddress);
        auditLog.setCreatedAt(new Date());
        this.save(auditLog);
    }
}
