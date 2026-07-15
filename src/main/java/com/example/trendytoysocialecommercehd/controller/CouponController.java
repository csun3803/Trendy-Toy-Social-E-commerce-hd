package com.example.trendytoysocialecommercehd.controller;

import com.example.trendytoysocialecommercehd.common.Result;
import com.example.trendytoysocialecommercehd.dto.AvailableCouponDTO;
import com.example.trendytoysocialecommercehd.dto.CouponTemplateRequest;
import com.example.trendytoysocialecommercehd.dto.IssueCouponRequest;
import com.example.trendytoysocialecommercehd.dto.UserCouponDTO;
import com.example.trendytoysocialecommercehd.entity.CouponTemplate;
import com.example.trendytoysocialecommercehd.entity.UserCoupon;
import com.example.trendytoysocialecommercehd.service.CouponService;
import com.example.trendytoysocialecommercehd.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/coupons")
public class CouponController {

    @Autowired
    private CouponService couponService;

    @Autowired
    private JwtUtil jwtUtil;

    private String getUserIdFromToken(String token) {
        String cleanToken = token.replace("Bearer ", "");
        return jwtUtil.getUserIdFromToken(cleanToken);
    }

    // ==================== 管理端：模板管理 ====================

    @GetMapping("/templates")
    public Result<List<CouponTemplate>> listTemplates(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String status) {
        try {
            return Result.success(couponService.listTemplates(name, status));
        } catch (Exception e) {
            return Result.error("获取模板列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/templates/{templateId}")
    public Result<CouponTemplate> getTemplate(@PathVariable String templateId) {
        try {
            CouponTemplate template = couponService.getTemplate(templateId);
            if (template == null) {
                return Result.error("模板不存在");
            }
            return Result.success(template);
        } catch (Exception e) {
            return Result.error("获取模板失败: " + e.getMessage());
        }
    }

    @PostMapping("/templates")
    public Result<CouponTemplate> createTemplate(@RequestBody CouponTemplateRequest request) {
        try {
            return Result.success(couponService.createTemplate(request));
        } catch (Exception e) {
            return Result.error("创建模板失败: " + e.getMessage());
        }
    }

    @PutMapping("/templates")
    public Result<CouponTemplate> updateTemplate(@RequestBody CouponTemplateRequest request) {
        try {
            return Result.success(couponService.updateTemplate(request));
        } catch (Exception e) {
            return Result.error("更新模板失败: " + e.getMessage());
        }
    }

    @PutMapping("/templates/{templateId}/status")
    public Result<CouponTemplate> toggleTemplateStatus(
            @PathVariable String templateId,
            @RequestBody java.util.Map<String, String> body) {
        try {
            String status = body.get("status");
            return Result.success(couponService.toggleTemplateStatus(templateId, status));
        } catch (Exception e) {
            return Result.error("修改模板状态失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/templates/{templateId}")
    public Result<Void> deleteTemplate(@PathVariable String templateId) {
        try {
            couponService.deleteTemplate(templateId);
            return Result.success();
        } catch (Exception e) {
            return Result.error("删除模板失败: " + e.getMessage());
        }
    }

    // ==================== 管理端：发券 ====================

    @PostMapping("/issue")
    public Result<List<UserCoupon>> issueCoupons(@RequestBody IssueCouponRequest request) {
        try {
            return Result.success(couponService.issueCoupons(request));
        } catch (Exception e) {
            return Result.error("发券失败: " + e.getMessage());
        }
    }

    // ==================== 管理端：券列表（全部用户） ====================

    @GetMapping("/list")
    public Result<List<UserCouponDTO>> listAllUserCoupons(
            @RequestParam(required = false) String templateId,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String status) {
        try {
            return Result.success(couponService.listAllUserCoupons(templateId, userId, status));
        } catch (Exception e) {
            return Result.error("获取券列表失败: " + e.getMessage());
        }
    }

    @PostMapping("/{userCouponId}/revoke")
    public Result<UserCoupon> revokeUserCoupon(@PathVariable String userCouponId) {
        try {
            return Result.success(couponService.revokeUserCoupon(userCouponId));
        } catch (Exception e) {
            return Result.error("作废券失败: " + e.getMessage());
        }
    }

    // ==================== 用户端：我的券包 ====================

    @GetMapping("/mine")
    public Result<List<UserCouponDTO>> getMyCoupons(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) String status) {
        try {
            String userId = getUserIdFromToken(token);
            return Result.success(couponService.getUserCoupons(userId, status));
        } catch (Exception e) {
            return Result.error("获取我的券包失败: " + e.getMessage());
        }
    }

    @GetMapping("/available")
    public Result<List<AvailableCouponDTO>> getAvailableCoupons(
            @RequestHeader("Authorization") String token,
            @RequestParam BigDecimal orderAmount) {
        try {
            String userId = getUserIdFromToken(token);
            return Result.success(couponService.getAvailableCoupons(userId, orderAmount));
        } catch (Exception e) {
            return Result.error("获取可用券失败: " + e.getMessage());
        }
    }
}
