package com.example.trendytoysocialecommercehd.controller;

import com.example.trendytoysocialecommercehd.common.Result;
import com.example.trendytoysocialecommercehd.entity.MyDisplayCabinet;
import com.example.trendytoysocialecommercehd.entity.CabinetItem;
import com.example.trendytoysocialecommercehd.service.CabinetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/cabinet")
public class CabinetController {

    @Autowired
    private CabinetService cabinetService;

    @GetMapping("/user/{userId}")
    public Result<List<MyDisplayCabinet>> getUserCabinets(@PathVariable String userId) {
        return Result.success(cabinetService.getCabinetsByUserId(userId));
    }

    @GetMapping("/{cabinetId}")
    public Result<MyDisplayCabinet> getCabinetDetail(@PathVariable String cabinetId) {
        MyDisplayCabinet cabinet = cabinetService.getCabinetDetail(cabinetId);
        if (cabinet == null) {
            return Result.error("盒柜不存在");
        }
        return Result.success(cabinet);
    }

    @GetMapping("/{cabinetId}/items")
    public Result<List<CabinetItem>> getCabinetItems(@PathVariable String cabinetId) {
        return Result.success(cabinetService.getCabinetItems(cabinetId));
    }

    @PostMapping
    public Result<MyDisplayCabinet> createCabinet(@RequestBody MyDisplayCabinet cabinet) {
        return Result.success(cabinetService.createCabinet(cabinet));
    }

    @PutMapping("/{cabinetId}")
    public Result<MyDisplayCabinet> updateCabinet(
            @PathVariable String cabinetId,
            @RequestBody MyDisplayCabinet cabinet) {
        return Result.success(cabinetService.updateCabinet(cabinetId, cabinet));
    }

    @DeleteMapping("/{cabinetId}")
    public Result<Void> deleteCabinet(@PathVariable String cabinetId) {
        cabinetService.deleteCabinet(cabinetId);
        return Result.success(null);
    }

    @PostMapping("/{cabinetId}/items")
    public Result<CabinetItem> addItemToCabinet(
            @PathVariable String cabinetId,
            @RequestBody CabinetItem item) {
        return Result.success(cabinetService.addItemToCabinet(cabinetId, item));
    }

    @PutMapping("/{cabinetId}/items/{itemId}")
    public Result<CabinetItem> updateCabinetItem(
            @PathVariable String cabinetId,
            @PathVariable String itemId,
            @RequestBody CabinetItem item) {
        return Result.success(cabinetService.updateCabinetItem(cabinetId, itemId, item));
    }

    @DeleteMapping("/{cabinetId}/items/{itemId}")
    public Result<Void> removeItemFromCabinet(
            @PathVariable String cabinetId,
            @PathVariable String itemId) {
        cabinetService.removeItemFromCabinet(cabinetId, itemId);
        return Result.success(null);
    }

    @PostMapping("/{cabinetId}/like")
    public Result<Void> likeCabinet(@PathVariable String cabinetId) {
        return Result.success(null);
    }

    @DeleteMapping("/{cabinetId}/like")
    public Result<Void> unlikeCabinet(@PathVariable String cabinetId) {
        return Result.success(null);
    }
}