package com.example.trendytoysocialecommercehd.controller;

import com.example.trendytoysocialecommercehd.common.Result;
import com.example.trendytoysocialecommercehd.entity.ShopAdmin;
import com.example.trendytoysocialecommercehd.entity.ShippingTemplate;
import com.example.trendytoysocialecommercehd.service.ShopAdminService;
import com.example.trendytoysocialecommercehd.service.ShippingTemplateService;
import com.example.trendytoysocialecommercehd.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 运费模板 Controller
 */
@RestController
@RequestMapping("/api/shipping-template")
@Tag(name = "运费模板管理", description = "商家端运费模板配置和运费计算")
public class ShippingTemplateController {

    @Autowired
    private ShippingTemplateService shippingTemplateService;

    @Autowired
    private ShopAdminService shopAdminService;

    @Autowired
    private JwtUtil jwtUtil;

    private String resolveShopId(String token) {
        if (token == null || token.isEmpty()) {
            throw new RuntimeException("未登录");
        }
        String cleanToken = token.replace("Bearer ", "");
        if (!jwtUtil.validateToken(cleanToken)) {
            throw new RuntimeException("无效的token");
        }
        String adminId = jwtUtil.getUserIdFromToken(cleanToken);
        ShopAdmin admin = shopAdminService.getShopAdminById(adminId);
        if (admin == null || admin.getShopId() == null || admin.getShopId().isEmpty()) {
            throw new RuntimeException("商家信息不存在或未关联店铺");
        }
        return admin.getShopId();
    }

    // ========== 商家端 CRUD ==========

    @GetMapping("/merchant/list")
    @Operation(summary = "商家端-查看运费模板列表")
    public Result<List<ShippingTemplate>> merchantList(@RequestHeader("Authorization") String token) {
        try {
            String shopId = resolveShopId(token);
            List<ShippingTemplate> templates = shippingTemplateService.getByShopId(shopId);
            return Result.success(templates);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/merchant")
    @Operation(summary = "商家端-创建运费模板")
    public Result<ShippingTemplate> merchantCreate(
            @RequestHeader("Authorization") String token,
            @RequestBody ShippingTemplate template) {
        try {
            String shopId = resolveShopId(token);
            template.setShopId(shopId);
            ShippingTemplate created = shippingTemplateService.create(template);
            return Result.success(created);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/merchant/{templateId}")
    @Operation(summary = "商家端-更新运费模板")
    public Result<ShippingTemplate> merchantUpdate(
            @RequestHeader("Authorization") String token,
            @PathVariable String templateId,
            @RequestBody ShippingTemplate template) {
        try {
            String shopId = resolveShopId(token);
            // 越权校验
            ShippingTemplate existing = shippingTemplateService.getById(templateId);
            if (existing == null || !existing.getShopId().equals(shopId)) {
                return Result.error("无权操作此模板");
            }
            template.setTemplateId(templateId);
            template.setShopId(shopId);
            ShippingTemplate updated = shippingTemplateService.update(template);
            return Result.success(updated);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/merchant/{templateId}")
    @Operation(summary = "商家端-删除运费模板")
    public Result<Void> merchantDelete(
            @RequestHeader("Authorization") String token,
            @PathVariable String templateId) {
        try {
            String shopId = resolveShopId(token);
            ShippingTemplate existing = shippingTemplateService.getById(templateId);
            if (existing == null || !existing.getShopId().equals(shopId)) {
                return Result.error("无权操作此模板");
            }
            shippingTemplateService.delete(templateId);
            return Result.success(null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    // ========== 公开端点 ==========

    @GetMapping("/calculate")
    @Operation(summary = "计算运费")
    public Result<BigDecimal> calculate(
            @RequestParam String shopId,
            @RequestParam String province,
            @RequestParam BigDecimal orderAmount) {
        try {
            BigDecimal fee = shippingTemplateService.calculateShippingFee(shopId, province, orderAmount);
            return Result.success(fee);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
