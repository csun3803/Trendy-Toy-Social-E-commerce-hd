package com.example.trendytoysocialecommercehd.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.trendytoysocialecommercehd.common.Result;
import com.example.trendytoysocialecommercehd.dto.MerchantApplyDTO;
import com.example.trendytoysocialecommercehd.dto.MerchantPhoneRegisterDTO;
import com.example.trendytoysocialecommercehd.entity.MerchantApplication;
import com.example.trendytoysocialecommercehd.entity.ShopAdmin;
import com.example.trendytoysocialecommercehd.service.MerchantApplicationService;
import com.example.trendytoysocialecommercehd.service.ShopAdminService;
import com.example.trendytoysocialecommercehd.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/merchant-application")
public class MerchantApplicationController {

    @Autowired
    private MerchantApplicationService merchantApplicationService;

    @Autowired
    private ShopAdminService shopAdminService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 商家注册（手机号+密码）
     */
    @PostMapping("/register")
    public Result<?> register(@RequestBody MerchantPhoneRegisterDTO dto) {
        try {
            if (!dto.getPassword().equals(dto.getConfirmPassword())) {
                return Result.error("两次密码输入不一致");
            }
            if (dto.getMobile() == null || !dto.getMobile().matches("^1[3-9]\\d{9}$")) {
                return Result.error("请输入正确的手机号");
            }
            ShopAdmin shopAdmin = merchantApplicationService.registerByPhone(dto.getMobile(), dto.getPassword());
            String token = jwtUtil.generateToken(shopAdmin.getAdminId());

            Map<String, Object> result = new HashMap<>();
            result.put("user", shopAdmin);
            result.put("token", token);
            return Result.success(result);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 商家提交入驻申请（已注册后填写信息）
     */
    @PostMapping("/submit")
    public Result<?> submitApplication(@RequestBody MerchantApplyDTO dto, @RequestHeader("Authorization") String token) {
        try {
            String cleanToken = token.replace("Bearer ", "");
            String adminId = jwtUtil.getUserIdFromToken(cleanToken);
            MerchantApplication app = merchantApplicationService.submitApplicationAfterRegister(dto, adminId);
            return Result.success(app);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 一步完成注册+申请
     */
    @PostMapping("/apply")
    public Result<?> apply(@RequestBody MerchantApplyDTO dto) {
        try {
            if (dto.getMobile() == null || !dto.getMobile().matches("^1[3-9]\\d{9}$")) {
                return Result.error("请输入正确的手机号");
            }
            MerchantApplication app = merchantApplicationService.submitApplication(dto);
            // 注册后自动登录
            ShopAdmin shopAdmin = shopAdminService.getById(dto.getMobile());
            String token = jwtUtil.generateToken(shopAdmin.getAdminId());

            Map<String, Object> result = new HashMap<>();
            result.put("application", app);
            result.put("user", shopAdmin);
            result.put("token", token);
            return Result.success(result);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 查询当前商家的申请状态
     */
    @GetMapping("/status")
    public Result<?> getApplicationStatus(@RequestHeader("Authorization") String token) {
        try {
            String cleanToken = token.replace("Bearer ", "");
            String adminId = jwtUtil.getUserIdFromToken(cleanToken);
            MerchantApplication app = merchantApplicationService.getApplicationByMobile(adminId);
            return Result.success(app);
        } catch (Exception e) {
            return Result.error("获取申请状态失败");
        }
    }

    // ==================== 管理员端接口 ====================

    /**
     * 获取入驻申请列表
     */
    @GetMapping("/admin/list")
    public Result<?> getApplicationList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status) {
        Page<MerchantApplication> result = merchantApplicationService.getApplicationList(page, size, status);
        return Result.success(result);
    }

    /**
     * 获取入驻申请详情
     */
    @GetMapping("/admin/{id}")
    public Result<?> getApplicationDetail(@PathVariable Long id) {
        MerchantApplication app = merchantApplicationService.getById(id);
        if (app == null) {
            return Result.error("申请不存在");
        }
        return Result.success(app);
    }

    /**
     * 审核通过
     */
    @PutMapping("/admin/{id}/approve")
    public Result<?> approveApplication(@PathVariable Long id, @RequestBody Map<String, String> params) {
        try {
            String auditorId = params.get("auditorId");
            merchantApplicationService.approveApplication(id, auditorId);
            return Result.success("审核通过");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 审核驳回
     */
    @PutMapping("/admin/{id}/reject")
    public Result<?> rejectApplication(@PathVariable Long id, @RequestBody Map<String, String> params) {
        try {
            String auditRemark = params.get("auditRemark");
            if (auditRemark == null || auditRemark.trim().isEmpty()) {
                return Result.error("驳回时必须填写原因");
            }
            merchantApplicationService.rejectApplication(id, auditRemark);
            return Result.success("已驳回");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }
}
