package com.example.trendytoysocialecommercehd.controller;

import com.example.trendytoysocialecommercehd.annotation.AuditLog;
import com.example.trendytoysocialecommercehd.common.Result;
import com.example.trendytoysocialecommercehd.dto.BlindBoxMachineStatisticsDTO;
import com.example.trendytoysocialecommercehd.entity.BlindBoxDrawRecord;
import com.example.trendytoysocialecommercehd.entity.BlindBoxMachine;
import com.example.trendytoysocialecommercehd.entity.Product;
import com.example.trendytoysocialecommercehd.service.BlindBoxMachineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理员端抽盒机监管 Controller
 * 路径前缀: /api/admin/blind-box-machine
 *
 * 注意：管理员仅有监管权限，不能创建/编辑抽盒机配置
 */
@RestController
@RequestMapping("/api/admin/blind-box-machine")
@Tag(name = "管理员端-抽盒机监管", description = "全平台抽盒机监管、审核、强制下架")
public class BlindBoxMachineAdminController {

    @Autowired
    private BlindBoxMachineService blindBoxMachineService;

    @GetMapping("/list")
    @Operation(summary = "管理员端-全平台抽盒机列表（支持按店铺/状态/审核状态/关键词筛选，分页返回）")
    public Result<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String shopId,
            @RequestParam(required = false) String machineStatus,
            @RequestParam(required = false) String auditStatus,
            @RequestParam(required = false) String keyword) {
        try {
            List<BlindBoxMachine> all = blindBoxMachineService.getAllMachines(
                    shopId, machineStatus, auditStatus, keyword);
            int total = all.size();
            int fromIndex = Math.max(0, (page - 1) * size);
            int toIndex = Math.min(fromIndex + size, total);
            List<BlindBoxMachine> records = fromIndex < toIndex
                    ? new ArrayList<>(all.subList(fromIndex, toIndex))
                    : new ArrayList<>();
            Map<String, Object> result = new HashMap<>();
            result.put("records", records);
            result.put("list", records);
            result.put("total", total);
            result.put("current", page);
            result.put("size", size);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("获取抽盒机列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/{machineId}")
    @Operation(summary = "管理员端-抽盒机详情")
    public Result<BlindBoxMachine> detail(@PathVariable String machineId) {
        try {
            BlindBoxMachine machine = blindBoxMachineService.getMachineDetail(machineId);
            return Result.success(machine);
        } catch (Exception e) {
            return Result.error("获取抽盒机详情失败: " + e.getMessage());
        }
    }

    @GetMapping("/{machineId}/variants")
    @Operation(summary = "管理员端-查看抽盒机款式列表（图鉴款式）")
    public Result<List<Product>> variants(@PathVariable String machineId) {
        try {
            List<Product> variants = blindBoxMachineService.getMachineVariants(machineId);
            return Result.success(variants);
        } catch (Exception e) {
            return Result.error("获取款式列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/{machineId}/records")
    @Operation(summary = "管理员端-查看抽盒机抽盒记录（分页）")
    public Result<Map<String, Object>> records(
            @PathVariable String machineId,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String drawType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            List<BlindBoxDrawRecord> all = blindBoxMachineService.getMachineRecords(machineId, userId, drawType);
            int total = all.size();
            int fromIndex = Math.max(0, (page - 1) * size);
            int toIndex = Math.min(fromIndex + size, total);
            List<BlindBoxDrawRecord> records = fromIndex < toIndex
                    ? new ArrayList<>(all.subList(fromIndex, toIndex))
                    : new ArrayList<>();
            Map<String, Object> result = new HashMap<>();
            result.put("records", records);
            result.put("list", records);
            result.put("total", total);
            result.put("current", page);
            result.put("size", size);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("获取抽盒记录失败: " + e.getMessage());
        }
    }

    @GetMapping("/{machineId}/statistics")
    @Operation(summary = "管理员端-抽盒机统计数据")
    public Result<BlindBoxMachineStatisticsDTO> statistics(@PathVariable String machineId) {
        try {
            BlindBoxMachineStatisticsDTO dto = blindBoxMachineService.getAdminMachineStatistics(machineId);
            return Result.success(dto);
        } catch (Exception e) {
            return Result.error("获取统计数据失败: " + e.getMessage());
        }
    }

    @AuditLog(module = "BLIND_BOX", action = "AUDIT", description = "审核抽盒机通过")
    @PutMapping("/{machineId}/approve")
    @Operation(summary = "管理员端-审核通过抽盒机")
    public Result<BlindBoxMachine> approve(@PathVariable String machineId) {
        try {
            BlindBoxMachine machine = blindBoxMachineService.approveMachine(machineId);
            return Result.success(machine);
        } catch (Exception e) {
            return Result.error("审核通过失败: " + e.getMessage());
        }
    }

    @AuditLog(module = "BLIND_BOX", action = "AUDIT", description = "审核抽盒机驳回")
    @PutMapping("/{machineId}/reject")
    @Operation(summary = "管理员端-审核驳回抽盒机（需提供驳回原因）")
    public Result<BlindBoxMachine> reject(
            @PathVariable String machineId,
            @RequestBody Map<String, String> body) {
        try {
            // 兼容前端 auditRemark 字段名，同时保留 remark 兼容旧调用
            String remark = body.get("auditRemark");
            if (remark == null || remark.trim().isEmpty()) {
                remark = body.get("remark");
            }
            BlindBoxMachine machine = blindBoxMachineService.rejectMachine(machineId, remark);
            return Result.success(machine);
        } catch (Exception e) {
            return Result.error("审核驳回失败: " + e.getMessage());
        }
    }

    @AuditLog(module = "BLIND_BOX", action = "TAKEDOWN", description = "强制下架抽盒机")
    @PutMapping("/{machineId}/takedown")
    @Operation(summary = "管理员端-强制下架违规抽盒机（需提供下架原因）")
    public Result<BlindBoxMachine> takedown(
            @PathVariable String machineId,
            @RequestBody Map<String, String> body) {
        try {
            // 兼容前端 auditRemark 字段名，同时保留 reason 兼容旧调用
            String reason = body.get("auditRemark");
            if (reason == null || reason.trim().isEmpty()) {
                reason = body.get("reason");
            }
            BlindBoxMachine machine = blindBoxMachineService.takedownMachine(machineId, reason);
            return Result.success(machine);
        } catch (Exception e) {
            return Result.error("强制下架失败: " + e.getMessage());
        }
    }

    @AuditLog(module = "BLIND_BOX", action = "STATUS", description = "管理员启用/禁用抽盒机")
    @PutMapping("/{machineId}/status")
    @Operation(summary = "管理员端-启用/禁用抽盒机")
    public Result<BlindBoxMachine> updateStatus(
            @PathVariable String machineId,
            @RequestBody Map<String, String> body) {
        try {
            String status = body.get("machineStatus");
            if (status == null || status.trim().isEmpty()) {
                status = body.get("status");
            }
            if (!"ACTIVE".equals(status) && !"INACTIVE".equals(status)) {
                return Result.error("管理员只能设置启用或禁用状态");
            }
            BlindBoxMachine machine = blindBoxMachineService.adminUpdateMachineStatus(machineId, status);
            return Result.success(machine);
        } catch (Exception e) {
            return Result.error("操作失败: " + e.getMessage());
        }
    }
}
