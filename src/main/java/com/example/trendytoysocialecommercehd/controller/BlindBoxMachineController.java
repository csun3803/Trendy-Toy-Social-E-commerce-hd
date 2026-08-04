package com.example.trendytoysocialecommercehd.controller;

import com.example.trendytoysocialecommercehd.common.Result;
import com.example.trendytoysocialecommercehd.dto.BlindBoxPickRequestDTO;
import com.example.trendytoysocialecommercehd.dto.BlindBoxPickResultDTO;
import com.example.trendytoysocialecommercehd.dto.DrawRequestDTO;
import com.example.trendytoysocialecommercehd.dto.DrawResultDTO;
import com.example.trendytoysocialecommercehd.entity.BlindBoxDrawRecord;
import com.example.trendytoysocialecommercehd.entity.BlindBoxMachine;
import com.example.trendytoysocialecommercehd.entity.BlindBoxSet;
import com.example.trendytoysocialecommercehd.entity.BlindBoxSlot;
import com.example.trendytoysocialecommercehd.entity.Product;
import com.example.trendytoysocialecommercehd.service.BlindBoxMachineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/blind-box")
@Tag(name = "抽盒机管理", description = "抽盒机相关接口")
public class BlindBoxMachineController {

    @Autowired
    private BlindBoxMachineService blindBoxMachineService;

    @GetMapping("/machines")
    @Operation(summary = "获取所有活跃的抽盒机列表")
    public Result<List<BlindBoxMachine>> getActiveMachines() {
        try {
            List<BlindBoxMachine> machines = blindBoxMachineService.getActiveMachines();
            return Result.success(machines);
        } catch (Exception e) {
            return Result.error("获取抽盒机列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/machines/{machineId}")
    @Operation(summary = "获取抽盒机详情")
    public Result<BlindBoxMachine> getMachineDetail(@PathVariable String machineId) {
        try {
            BlindBoxMachine machine = blindBoxMachineService.getMachineDetail(machineId);
            return Result.success(machine);
        } catch (Exception e) {
            return Result.error("获取抽盒机详情失败: " + e.getMessage());
        }
    }

    @GetMapping("/machines/{machineId}/variants")
    @Operation(summary = "获取抽盒机下的款式列表（图鉴款式）")
    public Result<List<Product>> getMachineVariants(@PathVariable String machineId) {
        try {
            List<Product> variants = blindBoxMachineService.getMachineVariants(machineId);
            return Result.success(variants);
        } catch (Exception e) {
            return Result.error("获取款式列表失败: " + e.getMessage());
        }
    }

    @PostMapping("/draw")
    @Operation(summary = "抽盒")
    public Result<DrawResultDTO> draw(@RequestBody DrawRequestDTO request) {
        try {
            DrawResultDTO result = blindBoxMachineService.draw(request);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("抽盒失败: " + e.getMessage());
        }
    }

    @GetMapping("/machines/{machineId}/slots")
    @Operation(summary = "获取九宫格选盒状态")
    public Result<List<BlindBoxSlot>> getMachineSlots(@PathVariable String machineId) {
        try {
            List<BlindBoxSlot> slots = blindBoxMachineService.getMachineSlots(machineId);
            return Result.success(slots);
        } catch (Exception e) {
            return Result.error("获取选盒状态失败: " + e.getMessage());
        }
    }

    @GetMapping("/machines/{machineId}/sets")
    @Operation(summary = "获取抽盒机的所有套盒（含格位信息，用于左右切换）")
    public Result<List<BlindBoxSet>> getMachineSets(@PathVariable String machineId) {
        try {
            List<BlindBoxSet> sets = blindBoxMachineService.getMachineSets(machineId);
            return Result.success(sets);
        } catch (Exception e) {
            return Result.error("获取套盒列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/sets/{setId}")
    @Operation(summary = "获取套盒详情（含格位）")
    public Result<BlindBoxSet> getSetDetail(@PathVariable String setId) {
        try {
            BlindBoxSet set = blindBoxMachineService.getSetDetail(setId);
            return Result.success(set);
        } catch (Exception e) {
            return Result.error("获取套盒详情失败: " + e.getMessage());
        }
    }

    @PostMapping("/machines/{machineId}/queue")
    @Operation(summary = "加入排队")
    public Result<Map<String, Object>> joinQueue(
            @PathVariable String machineId,
            @RequestBody Map<String, String> body) {
        try {
            String userId = body.get("userId");
            Map<String, Object> result = blindBoxMachineService.joinQueue(machineId, userId);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("加入排队失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/machines/{machineId}/queue")
    @Operation(summary = "离开排队")
    public Result<Void> leaveQueue(
            @PathVariable String machineId,
            @RequestBody Map<String, String> body) {
        try {
            String userId = body.get("userId");
            blindBoxMachineService.leaveQueue(machineId, userId);
            return Result.success();
        } catch (Exception e) {
            return Result.error("离开排队失败: " + e.getMessage());
        }
    }

    @GetMapping("/machines/{machineId}/queue/status")
    @Operation(summary = "查询用户排队状态")
    public Result<Map<String, Object>> getQueueStatus(
            @PathVariable String machineId,
            @RequestParam String userId) {
        try {
            Map<String, Object> result = blindBoxMachineService.getQueueStatus(machineId, userId);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("查询排队状态失败: " + e.getMessage());
        }
    }

    @PostMapping("/pick")
    @Operation(summary = "选盒购买（选中某个盒子立即购买并揭晓）")
    public Result<BlindBoxPickResultDTO> pickBlindBox(@RequestBody BlindBoxPickRequestDTO request) {
        try {
            BlindBoxPickResultDTO result = blindBoxMachineService.pickBlindBox(request);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("选盒购买失败: " + e.getMessage());
        }
    }

    @GetMapping("/machines/{machineId}/history")
    @Operation(summary = "获取用户抽盒历史")
    public Result<List<BlindBoxDrawRecord>> getUserDrawHistory(
            @PathVariable String machineId,
            @RequestParam String userId) {
        try {
            List<BlindBoxDrawRecord> records = blindBoxMachineService.getUserDrawHistory(machineId, userId);
            return Result.success(records);
        } catch (Exception e) {
            return Result.error("获取抽盒历史失败: " + e.getMessage());
        }
    }

    @GetMapping("/draw-records")
    @Operation(summary = "获取用户所有抽盒记录")
    public Result<List<BlindBoxDrawRecord>> getUserDrawRecords(
            @RequestParam String userId,
            @RequestParam(required = false) String machineId) {
        try {
            List<BlindBoxDrawRecord> records;
            if (machineId != null && !machineId.isEmpty()) {
                records = blindBoxMachineService.getUserMachineDrawRecords(userId, machineId);
            } else {
                records = blindBoxMachineService.getUserAllDrawRecords(userId);
            }
            return Result.success(records);
        } catch (Exception e) {
            return Result.error("获取抽盒记录失败: " + e.getMessage());
        }
    }

    @PostMapping("/draw-records/{recordId}/open")
    @Operation(summary = "开盒")
    public Result<BlindBoxDrawRecord> openBox(
            @PathVariable String recordId,
            @RequestBody Map<String, String> body) {
        try {
            String userId = body.get("userId");
            BlindBoxDrawRecord record = blindBoxMachineService.openBox(recordId, userId);
            return Result.success(record);
        } catch (Exception e) {
            return Result.error("开盒失败: " + e.getMessage());
        }
    }

    @GetMapping("/luck-ranking")
    @Operation(summary = "欧气排行榜")
    public Result<List<Map<String, Object>>> getLuckRanking(
            @RequestParam(required = false, defaultValue = "50") int limit) {
        try {
            List<Map<String, Object>> ranking = blindBoxMachineService.getLuckRanking(limit);
            return Result.success(ranking);
        } catch (Exception e) {
            return Result.error("获取排行榜失败: " + e.getMessage());
        }
    }

    @PostMapping("/machines")
    @Operation(summary = "创建抽盒机")
    public Result<BlindBoxMachine> createMachine(@RequestBody Map<String, Object> body) {
        try {
            BlindBoxMachine machine = new BlindBoxMachine();
            if (body.get("shopId") != null) {
                machine.setShopId((String) body.get("shopId"));
            }
            if (body.get("machineName") != null) {
                machine.setMachineName((String) body.get("machineName"));
            }
            if (body.get("machineDescription") != null) {
                machine.setMachineDescription((String) body.get("machineDescription"));
            }
            if (body.get("machineCoverImage") != null) {
                machine.setMachineCoverImage((String) body.get("machineCoverImage"));
            }
            if (body.get("drawPrice") != null) {
                machine.setDrawPrice(new java.math.BigDecimal(body.get("drawPrice").toString()));
            }
            if (body.get("tenDrawPrice") != null) {
                machine.setTenDrawPrice(new java.math.BigDecimal(body.get("tenDrawPrice").toString()));
            }
            BlindBoxMachine created = blindBoxMachineService.createMachine(machine);
            return Result.success(created);
        } catch (Exception e) {
            return Result.error("创建抽盒机失败: " + e.getMessage());
        }
    }

    @PutMapping("/machines/{machineId}")
    @Operation(summary = "更新抽盒机")
    public Result<BlindBoxMachine> updateMachine(
            @PathVariable String machineId,
            @RequestBody Map<String, Object> body) {
        try {
            BlindBoxMachine machine = new BlindBoxMachine();
            machine.setMachineId(machineId);
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
                machine.setDrawPrice(new java.math.BigDecimal(body.get("drawPrice").toString()));
            }
            if (body.containsKey("tenDrawPrice") && body.get("tenDrawPrice") != null) {
                machine.setTenDrawPrice(new java.math.BigDecimal(body.get("tenDrawPrice").toString()));
            }
            BlindBoxMachine updated = blindBoxMachineService.updateMachine(machineId, machine);
            return Result.success(updated);
        } catch (Exception e) {
            return Result.error("更新抽盒机失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/machines/{machineId}")
    @Operation(summary = "删除抽盒机")
    public Result<Void> deleteMachine(@PathVariable String machineId) {
        try {
            boolean success = blindBoxMachineService.deleteMachine(machineId);
            if (success) {
                return Result.success();
            }
            return Result.error("删除失败");
        } catch (Exception e) {
            return Result.error("删除抽盒机失败: " + e.getMessage());
        }
    }
}
