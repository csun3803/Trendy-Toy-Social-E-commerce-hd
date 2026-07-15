package com.example.trendytoysocialecommercehd.aspect;

import com.example.trendytoysocialecommercehd.annotation.AuditLog;
import com.example.trendytoysocialecommercehd.common.Result;
import com.example.trendytoysocialecommercehd.entity.PlatformAdmin;
import com.example.trendytoysocialecommercehd.entity.ShopAdmin;
import com.example.trendytoysocialecommercehd.service.AuditLogService;
import com.example.trendytoysocialecommercehd.service.PlatformAdminService;
import com.example.trendytoysocialecommercehd.service.ShopAdminService;
import com.example.trendytoysocialecommercehd.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

@Aspect
@Component
public class AuditLogAspect {

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PlatformAdminService platformAdminService;

    @Autowired
    private ShopAdminService shopAdminService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Around("@annotation(com.example.trendytoysocialecommercehd.annotation.AuditLog)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取注解
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        AuditLog auditLogAnnotation = method.getAnnotation(AuditLog.class);

        // 获取请求信息
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes != null ? attributes.getRequest() : null;

        String operatorId = null;
        String operatorName = null;
        String operatorType = "UNKNOWN";
        String ipAddress = null;

        if (request != null) {
            // 从JWT获取操作人信息
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                if (jwtUtil.validateToken(token)) {
                    operatorId = jwtUtil.getUserIdFromToken(token);
                    // 根据请求路径判断操作人类型
                    String uri = request.getRequestURI();
                    if (uri.contains("/api/admin/") || uri.contains("/api/merchant-manage/") || uri.contains("/api/user-manage/") || uri.contains("/api/audit-log/")) {
                        operatorType = "PLATFORM_ADMIN";
                        try {
                            PlatformAdmin admin = platformAdminService.getById(operatorId);
                            if (admin != null) {
                                operatorName = admin.getEmployeeId() != null ? admin.getEmployeeId() : admin.getAdminId();
                            }
                        } catch (Exception ignored) {}
                    } else if (uri.contains("/api/merchant/") || uri.contains("/api/shop/")) {
                        operatorType = "SHOP_ADMIN";
                        try {
                            ShopAdmin admin = shopAdminService.getById(operatorId);
                            if (admin != null) {
                                operatorName = admin.getAdminId();
                            }
                        } catch (Exception ignored) {}
                    }
                }
            }
            ipAddress = getClientIpAddress(request);
        }

        // 构建请求参数
        String requestParams = null;
        try {
            Object[] args = joinPoint.getArgs();
            if (args != null && args.length > 0) {
                Map<String, Object> paramsMap = new HashMap<>();
                String[] paramNames = signature.getParameterNames();
                for (int i = 0; i < args.length; i++) {
                    if (args[i] instanceof HttpServletRequest) continue;
                    String paramName = paramNames != null && i < paramNames.length ? paramNames[i] : "arg" + i;
                    paramsMap.put(paramName, args[i]);
                }
                if (!paramsMap.isEmpty()) {
                    String json = objectMapper.writeValueAsString(paramsMap);
                    // 截断过长的参数
                    requestParams = json.length() > 2000 ? json.substring(0, 2000) : json;
                }
            }
        } catch (Exception e) {
            requestParams = "参数序列化失败";
        }

        // 执行方法
        Integer responseCode = 200;
        Object result = null;
        try {
            result = joinPoint.proceed();
            // 对于登录操作，从返回结果中提取操作人信息
            if ("LOGIN".equals(auditLogAnnotation.action()) && result instanceof Result) {
                Result<?> apiResult = (Result<?>) result;
                Object data = apiResult.getData();
                if (data instanceof Map) {
                    Map<?, ?> dataMap = (Map<?, ?>) data;
                    Object userObj = dataMap.get("user");
                    if (userObj instanceof PlatformAdmin) {
                        PlatformAdmin admin = (PlatformAdmin) userObj;
                        operatorId = admin.getAdminId();
                        operatorName = admin.getEmployeeId();
                        operatorType = "PLATFORM_ADMIN";
                    } else if (userObj instanceof ShopAdmin) {
                        ShopAdmin admin = (ShopAdmin) userObj;
                        operatorId = admin.getAdminId();
                        operatorName = admin.getAdminId();
                        operatorType = "SHOP_ADMIN";
                    }
                }
            }
            return result;
        } catch (Exception e) {
            responseCode = 500;
            throw e;
        } finally {
            // 异步记录日志
            try {
                String module = auditLogAnnotation.module();
                String action = auditLogAnnotation.action();
                String description = auditLogAnnotation.description();
                String requestUrl = request != null ? request.getRequestURI() : "";
                String httpMethod = request != null ? request.getMethod() : "";

                // 从参数中提取目标ID
                String targetId = extractTargetId(joinPoint.getArgs(), signature);

                auditLogService.log(
                    operatorId, operatorName, operatorType,
                    action, module, description,
                    targetId, "",
                    httpMethod, requestUrl, requestParams,
                    responseCode, ipAddress
                );
            } catch (Exception e) {
                // 日志记录失败不影响业务
                System.err.println("审计日志记录失败: " + e.getMessage());
            }
        }
    }

    /**
     * 从方法参数中提取目标ID（路径变量中的ID）
     */
    private String extractTargetId(Object[] args, MethodSignature signature) {
        if (args == null || args.length == 0) return null;
        String[] paramNames = signature.getParameterNames();
        for (int i = 0; i < args.length; i++) {
            String paramName = paramNames != null && i < paramNames.length ? paramNames[i] : "";
            if (args[i] instanceof String && (paramName.contains("Id") || paramName.contains("id"))) {
                return (String) args[i];
            }
        }
        return null;
    }

    /**
     * 获取客户端真实IP
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多个代理时取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
