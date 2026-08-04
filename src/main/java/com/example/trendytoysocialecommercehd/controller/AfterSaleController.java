package com.example.trendytoysocialecommercehd.controller;

import com.example.trendytoysocialecommercehd.dto.AfterSaleInfoDTO;
import com.example.trendytoysocialecommercehd.dto.CreateAfterSaleRequest;
import com.example.trendytoysocialecommercehd.entity.AfterSale;
import com.example.trendytoysocialecommercehd.service.AfterSaleService;
import com.example.trendytoysocialecommercehd.common.Result;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/after-sale")
public class AfterSaleController {

    @Autowired
    private AfterSaleService afterSaleService;

    @PostMapping
    public Result<AfterSale> createAfterSale(@RequestBody CreateAfterSaleRequest request) {
        try {
            // 从订单获取用户ID
            AfterSale afterSale = afterSaleService.createAfterSale(request, "temp-user-id");
            return Result.success(afterSale);
        } catch (Exception e) {
            return Result.error("创建售后申请失败: " + e.getMessage());
        }
    }

    @GetMapping("/user/{userId}")
    public Result<List<AfterSaleInfoDTO>> getUserAfterSales(@PathVariable String userId) {
        try {
            List<AfterSaleInfoDTO> afterSales = afterSaleService.getAfterSalesByUserId(userId);
            return Result.success(afterSales);
        } catch (Exception e) {
            return Result.error("获取售后列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/order/{orderId}")
    public Result<List<AfterSaleInfoDTO>> getOrderAfterSales(@PathVariable String orderId) {
        try {
            List<AfterSaleInfoDTO> afterSales = afterSaleService.getAfterSalesByOrderId(orderId);
            return Result.success(afterSales);
        } catch (Exception e) {
            return Result.error("获取订单售后列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/seller/{sellerId}")
    public Result<List<AfterSaleInfoDTO>> getSellerAfterSales(@PathVariable String sellerId) {
        try {
            List<AfterSaleInfoDTO> afterSales = afterSaleService.getAfterSalesBySellerId(sellerId);
            return Result.success(afterSales);
        } catch (Exception e) {
            return Result.error("获取商家售后列表失败: " + e.getMessage());
        }
    }

    /**
     * 商家端售后列表查询（支持状态过滤）
     */
    @GetMapping("/seller/{sellerId}/list")
    public Result<List<AfterSaleInfoDTO>> getSellerAfterSalesWithFilter(
            @PathVariable String sellerId,
            @RequestParam(required = false) String status) {
        try {
            List<AfterSaleInfoDTO> afterSales = afterSaleService.getAfterSalesBySellerId(sellerId, status);
            return Result.success(afterSales);
        } catch (Exception e) {
            return Result.error("获取商家售后列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/{afterSaleId}")
    public Result<AfterSaleInfoDTO> getAfterSaleDetail(@PathVariable String afterSaleId) {
        try {
            AfterSaleInfoDTO afterSale = afterSaleService.getAfterSaleDetail(afterSaleId);
            return Result.success(afterSale);
        } catch (Exception e) {
            return Result.error("获取售后详情失败: " + e.getMessage());
        }
    }

    @PostMapping("/{afterSaleId}/approve")
    public Result<AfterSale> approveAfterSale(@PathVariable String afterSaleId) {
        try {
            AfterSale afterSale = afterSaleService.approveAfterSale(afterSaleId);
            return Result.success(afterSale);
        } catch (Exception e) {
            return Result.error("同意售后申请失败: " + e.getMessage());
        }
    }

    @PostMapping("/{afterSaleId}/reject")
    public Result<AfterSale> rejectAfterSale(@PathVariable String afterSaleId, @RequestBody RejectRequest request) {
        try {
            AfterSale afterSale = afterSaleService.rejectAfterSale(afterSaleId, request.getRejectReason());
            return Result.success(afterSale);
        } catch (Exception e) {
            return Result.error("拒绝售后申请失败: " + e.getMessage());
        }
    }

    @PostMapping("/{afterSaleId}/submit-logistics")
    public Result<AfterSale> submitReturnLogistics(@PathVariable String afterSaleId, @RequestBody SubmitLogisticsRequest request) {
        try {
            AfterSale afterSale = afterSaleService.submitReturnLogistics(
                afterSaleId, 
                request.getLogisticsCompany(), 
                request.getTrackingNumber()
            );
            return Result.success(afterSale);
        } catch (Exception e) {
            return Result.error("提交退货物流失败: " + e.getMessage());
        }
    }

    @PostMapping("/{afterSaleId}/confirm-return-received")
    public Result<AfterSale> confirmReturnReceived(@PathVariable String afterSaleId) {
        try {
            AfterSale afterSale = afterSaleService.confirmReturnReceived(afterSaleId);
            return Result.success(afterSale);
        } catch (Exception e) {
            return Result.error("确认退货收货失败: " + e.getMessage());
        }
    }

    /**
     * 商家填写退货地址（同意退货类售后后）
     */
    @PostMapping("/{afterSaleId}/fill-return-address")
    public Result<AfterSale> fillReturnAddress(
            @PathVariable String afterSaleId,
            @RequestBody FillReturnAddressRequest request,
            @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            // 简单权限校验：从token或request中提取sellerId（这里简化处理，实际由拦截器保证登录态）
            AfterSale afterSale = afterSaleService.fillReturnAddress(afterSaleId, request.getReturnAddress(), request.getSellerId());
            return Result.success(afterSale);
        } catch (Exception e) {
            return Result.error("填写退货地址失败: " + e.getMessage());
        }
    }

    /**
     * 用户申请平台介入（仅当商家拒绝售后申请后才能调用）
     */
    @PostMapping("/{afterSaleId}/apply-intervention")
    public Result<AfterSale> applyPlatformIntervention(
            @PathVariable String afterSaleId,
            @RequestBody ApplyInterventionRequest request) {
        try {
            AfterSale afterSale = afterSaleService.applyPlatformIntervention(afterSaleId, request.getReason(), request.getUserId());
            return Result.success(afterSale);
        } catch (Exception e) {
            return Result.error("申请平台介入失败: " + e.getMessage());
        }
    }

    /**
     * 用户取消售后申请（仅PENDING状态可取消）
     */
    @PostMapping("/{afterSaleId}/cancel")
    public Result<AfterSale> cancelAfterSale(@PathVariable String afterSaleId) {
        try {
            AfterSale afterSale = afterSaleService.cancelAfterSale(afterSaleId);
            return Result.success(afterSale);
        } catch (Exception e) {
            return Result.error("取消售后申请失败: " + e.getMessage());
        }
    }

    @Data
    public static class RejectRequest {
        private String rejectReason;
    }

    @Data
    public static class SubmitLogisticsRequest {
        private String logisticsCompany;
        private String trackingNumber;
    }

    @Data
    public static class FillReturnAddressRequest {
        private String returnAddress;
        private String sellerId;
    }

    @Data
    public static class ApplyInterventionRequest {
        private String reason;
        private String userId;
    }
}
