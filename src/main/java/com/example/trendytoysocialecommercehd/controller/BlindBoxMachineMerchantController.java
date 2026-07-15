package com.example.trendytoysocialecommercehd.controller;

import com.example.trendytoysocialecommercehd.common.Result;
import com.example.trendytoysocialecommercehd.dto.BlindBoxMachineStatisticsDTO;
import com.example.trendytoysocialecommercehd.entity.BlindBoxDrawRecord;
import com.example.trendytoysocialecommercehd.entity.BlindBoxMachine;
import com.example.trendytoysocialecommercehd.entity.BlindBoxMachineVariant;
import com.example.trendytoysocialecommercehd.entity.ShopAdmin;
import com.example.trendytoysocialecommercehd.service.BlindBoxMachineService;
import com.example.trendytoysocialecommercehd.service.ShopAdminService;
import com.example.trendytoysocialecommercehd.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.math.BigDecimal;

/**
 * 商家端抽盒机管理 Controller
 * 路径前缀: /api/blind-box-machine/merchant
 *
 * 注意：商家端只能管理本店铺的抽盒机，所有写操作均通过 JWT 解析 shopId 进行越权校验
 */
@RestController
@RequestMapping("/api/blind-box-machine/merchant")
@Tag(name = "商家端-抽盒机管理", description = "商家端抽盒机创建、编辑、状态管理、数据统计")
public class BlindBoxMachineMerchantController {

    @Autowired
    private BlindBoxMachineService blindBoxMachineService;

    @Autowired
    private ShopAdminService shopAdminService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 从 JWT 中解析商家 adminId，再查询其 shopId
     */
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

    @GetMapping("/list")
    @Operation(summary = "商家端-抽盒机列表（支持筛选和搜索）")
    public Result<List<BlindBoxMachine>> list(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String machineStatus,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String auditStatus) {
        try {
            String shopId = resolveShopId(token);
            // 兼容前端 status 字段名（前端列表筛选使用 status 而非 machineStatus）
            String finalStatus = (machineStatus != null && !machineStatus.isEmpty())
                    ? machineStatus
                    : status;
            List<BlindBoxMachine> list = blindBoxMachineService.getMerchantMachines(
                    shopId, keyword, finalStatus, auditStatus);
            return Result.success(list);
        } catch (Exception e) {
            return Result.error("获取抽盒机列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/{machineId}")
    @Operation(summary = "商家端-抽盒机详情")
    public Result<BlindBoxMachine> detail(
            @RequestHeader("Authorization") String token,
            @PathVariable String machineId) {
        try {
            String shopId = resolveShopId(token);
            BlindBoxMachine machine = blindBoxMachineService.getMerchantMachine(machineId, shopId);
            return Result.success(machine);
        } catch (Exception e) {
            return Result.error("获取抽盒机详情失败: " + e.getMessage());
        }
    }

    @PostMapping
    @Operation(summary = "商家端-创建抽盒机")
    public Result<BlindBoxMachine> create(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, Object> body) {
        try {
            String shopId = resolveShopId(token);

            // 从请求体手动构建实体，避免 JSON 反序列化问题
            System.out.println("[DEBUG] create body keys: " + body.keySet());
            System.out.println("[DEBUG] create body: " + body);
            BlindBoxMachine machine = new BlindBoxMachine();
            machine.setShopId(shopId);
            machine.setSaleSeriesId((String) body.get("saleSeriesId"));
            machine.setMachineName((String) body.get("machineName"));
            machine.setMachineDescription((String) body.get("machineDescription"));
            machine.setMachineCoverImage((String) body.get("machineCoverImage"));
            if (body.get("drawPrice") != null) {
                machine.setDrawPrice(new BigDecimal(body.get("drawPrice").toString()));
            }
            if (body.get("tenDrawPrice") != null) {
                machine.setTenDrawPrice(new BigDecimal(body.get("tenDrawPrice").toString()));
            }
            if (body.get("sortOrder") != null) {
                machine.setSortOrder(Integer.valueOf(body.get("sortOrder").toString()));
            }
            if (body.get("guaranteeDraws") != null) {
                machine.setGuaranteeDraws(Integer.valueOf(body.get("guaranteeDraws").toString()));
            }

            BlindBoxMachine created = blindBoxMachineService.createMachine(machine);
            return Result.success(created);
        } catch (Exception e) {
            return Result.error("创建抽盒机失败: " + e.getMessage());
        }
    }

    @PutMapping("/{machineId}")
    @Operation(summary = "商家端-更新抽盒机基础信息")
    public Result<BlindBoxMachine> update(
            @RequestHeader("Authorization") String token,
            @PathVariable String machineId,
            @RequestBody Map<String, Object> body) {
        try {
            String shopId = resolveShopId(token);
            // 越权校验
            blindBoxMachineService.getMerchantMachine(machineId, shopId);

            // 从请求体手动构建实体，仅允许修改基础字段
            BlindBoxMachine machine = new BlindBoxMachine();
            machine.setMachineId(machineId);
            machine.setShopId(shopId);
            if (body.containsKey("saleSeriesId")) {
                machine.setSaleSeriesId((String) body.get("saleSeriesId"));
            }
            if (body.containsKey("machineName")) {
                machine.setMachineName((String) body.get("machineName"));
            }
            if (body.containsKey("machineDescription")) {
                machine.setMachineDescription((String) body.get("machineDescription"));
            }
            if (body.containsKey("machineCoverImage")) {
                machine.setMachineCoverImage((String) body.get("machineCoverImage"));
            }
            if (body.get("drawPrice") != null) {
                machine.setDrawPrice(new BigDecimal(body.get("drawPrice").toString()));
            }
            if (body.containsKey("tenDrawPrice") && body.get("tenDrawPrice") != null) {
                machine.setTenDrawPrice(new BigDecimal(body.get("tenDrawPrice").toString()));
            }
            if (body.get("sortOrder") != null) {
                machine.setSortOrder(Integer.valueOf(body.get("sortOrder").toString()));
            }
            // 审核相关字段和统计字段禁止修改（不设置即可）

            BlindBoxMachine updated = blindBoxMachineService.updateMachine(machineId, machine);
            return Result.success(updated);
        } catch (Exception e) {
            return Result.error("更新抽盒机失败: " + e.getMessage());
        }
    }

    @GetMapping("/{machineId}/variants")
    @Operation(summary = "商家端-获取抽盒机款式覆盖配置（默认复用 sale_variant 数据）")
    public Result<List<BlindBoxMachineVariant>> getVariantsConfig(
            @RequestHeader("Authorization") String token,
            @PathVariable String machineId) {
        try {
            String shopId = resolveShopId(token);
            blindBoxMachineService.getMerchantMachine(machineId, shopId);
            List<BlindBoxMachineVariant> variants = blindBoxMachineService.getMachineVariantsConfig(machineId);
            return Result.success(variants);
        } catch (Exception e) {
            return Result.error("获取款式配置失败: " + e.getMessage());
        }
    }

    @PutMapping("/{machineId}/variants")
    @Operation(summary = "商家端-保存抽盒机款式覆盖配置")
    public Result<Void> saveVariantsConfig(
            @RequestHeader("Authorization") String token,
            @PathVariable String machineId,
            @RequestBody List<BlindBoxMachineVariant> variants) {
        try {
            String shopId = resolveShopId(token);
            blindBoxMachineService.getMerchantMachine(machineId, shopId);
            blindBoxMachineService.saveMachineVariants(machineId, variants);
            return Result.success();
        } catch (Exception e) {
            return Result.error("保存款式配置失败: " + e.getMessage());
        }
    }

    @PutMapping("/{machineId}/status")
    @Operation(summary = "商家端-启用/停用抽盒机")
    public Result<BlindBoxMachine> updateStatus(
            @RequestHeader("Authorization") String token,
            @PathVariable String machineId,
            @RequestBody Map<String, String> body) {
        try {
            String shopId = resolveShopId(token);
            // 兼容前端 machineStatus 字段名，同时保留 status 兼容旧调用
            String status = body.get("machineStatus");
            if (status == null || status.trim().isEmpty()) {
                status = body.get("status");
            }
            BlindBoxMachine machine = blindBoxMachineService.updateMachineStatus(machineId, shopId, status);
            return Result.success(machine);
        } catch (Exception e) {
            return Result.error("更新状态失败: " + e.getMessage());
        }
    }

    @PutMapping("/{machineId}/submit-audit")
    @Operation(summary = "商家端-提交审核")
    public Result<BlindBoxMachine> submitAudit(
            @RequestHeader("Authorization") String token,
            @PathVariable String machineId) {
        try {
            String shopId = resolveShopId(token);
            BlindBoxMachine machine = blindBoxMachineService.submitForAudit(machineId, shopId);
            return Result.success(machine);
        } catch (Exception e) {
            return Result.error("提交审核失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{machineId}")
    @Operation(summary = "商家端-删除抽盒机")
    public Result<Void> delete(
            @RequestHeader("Authorization") String token,
            @PathVariable String machineId) {
        try {
            String shopId = resolveShopId(token);
            // 越权校验
            blindBoxMachineService.getMerchantMachine(machineId, shopId);
            boolean success = blindBoxMachineService.deleteMachine(machineId);
            if (success) {
                return Result.success();
            }
            return Result.error("删除失败");
        } catch (Exception e) {
            return Result.error("删除抽盒机失败: " + e.getMessage());
        }
    }

    @GetMapping("/{machineId}/statistics")
    @Operation(summary = "商家端-抽盒机统计数据")
    public Result<BlindBoxMachineStatisticsDTO> statistics(
            @RequestHeader("Authorization") String token,
            @PathVariable String machineId) {
        try {
            String shopId = resolveShopId(token);
            BlindBoxMachineStatisticsDTO dto = blindBoxMachineService.getMachineStatistics(machineId, shopId);
            return Result.success(dto);
        } catch (Exception e) {
            return Result.error("获取统计数据失败: " + e.getMessage());
        }
    }

    @GetMapping("/{machineId}/records")
    @Operation(summary = "商家端-抽盒机抽盒记录")
    public Result<List<BlindBoxDrawRecord>> records(
            @RequestHeader("Authorization") String token,
            @PathVariable String machineId,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String drawType) {
        try {
            String shopId = resolveShopId(token);
            // 越权校验
            blindBoxMachineService.getMerchantMachine(machineId, shopId);
            List<BlindBoxDrawRecord> records = blindBoxMachineService.getMachineRecords(machineId, userId, drawType);
            return Result.success(records);
        } catch (Exception e) {
            return Result.error("获取抽盒记录失败: " + e.getMessage());
        }
    }
}
