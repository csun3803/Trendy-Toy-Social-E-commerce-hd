package com.example.trendytoysocialecommercehd.controller;

import com.example.trendytoysocialecommercehd.common.Result;
import com.example.trendytoysocialecommercehd.dto.OrderListDTO;
import com.example.trendytoysocialecommercehd.entity.BlindBoxDrawRecord;
import com.example.trendytoysocialecommercehd.entity.OrderItem;
import com.example.trendytoysocialecommercehd.mapper.BlindBoxDrawRecordMapper;
import com.example.trendytoysocialecommercehd.mapper.SaleVariantMapper;
import com.example.trendytoysocialecommercehd.mapper.SeriesMapper;
import com.example.trendytoysocialecommercehd.mapper.UserProductFavoriteMapper;
import com.example.trendytoysocialecommercehd.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * AI 工具数据接口
 * 为 Python ai-service 提供 Function Call 所需的实时数据查询能力。
 * 知识库只放静态知识（FAQ/百科/售后政策），实时数据全部走本接口。
 *
 * 接口清单：
 *   GET /api/ai/tools/logistics?orderId=xxx        查订单物流
 *   GET /api/ai/tools/series/info?seriesName=xxx   查系列摘要(供前端渲染卡片)
 *   GET /api/ai/tools/series/styles?seriesName=xxx  查系列款式列表
 *   GET /api/ai/tools/orders?userId=xxx            查用户订单列表
 *   GET /api/ai/tools/favorites?userId=xxx          查用户收藏列表
 *   GET /api/ai/tools/draw-history?userId=xxx      查用户抽盒历史
 *   GET /api/ai/tools/style-stock?styleName=xxx     查款式库存
 */
@Slf4j
@RestController
@RequestMapping("/api/ai/tools")
@RequiredArgsConstructor
public class AiToolsController {

    private final OrderService orderService;
    private final BlindBoxDrawRecordMapper blindBoxDrawRecordMapper;
    private final UserProductFavoriteMapper userProductFavoriteMapper;
    private final SaleVariantMapper saleVariantMapper;
    private final SeriesMapper seriesMapper;

    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * 1. 查订单物流
     * 用户问“订单到哪了/我的快递/物流”时调用
     */
    @GetMapping("/logistics")
    public Result<Map<String, Object>> queryLogistics(@RequestParam String orderId) {
        try {
            if (orderId == null || orderId.trim().isEmpty()) {
                return Result.error("orderId 不能为空");
            }
            OrderService.LogisticsInfo info = orderService.getLogisticsInfo(orderId);
            if (info == null) {
                return Result.error("未找到订单物流信息");
            }
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("orderId", info.getOrderId());
            data.put("logisticsCompany", info.getLogisticsCompany());
            data.put("trackingNumber", info.getTrackingNumber());
            List<Map<String, Object>> tracks = new ArrayList<>();
            if (info.getTracks() != null) {
                for (OrderService.LogisticsTrack t : info.getTracks()) {
                    Map<String, Object> tk = new LinkedHashMap<>();
                    tk.put("time", t.getTime());
                    tk.put("status", t.getStatus());
                    tk.put("description", t.getDescription());
                    tracks.add(tk);
                }
            }
            data.put("tracks", tracks);
            return Result.success(data);
        } catch (Exception e) {
            log.warn("[AiTools] logistics 查询失败 orderId={}: {}", orderId, e.getMessage());
            return Result.error("查询物流失败: " + e.getMessage());
        }
    }

    /**
     * 2. 查系列摘要信息（供前端渲染可点击的系列卡片）
     * 用户问“XX系列”、“XX款式”、“XXIP”时调用，返回系列ID/名称/款式数/封面等
     * 前端收到后渲染为卡片，点击跳转到 /series/{seriesId}
     */
    @GetMapping("/series/info")
    public Result<List<Map<String, Object>>> querySeriesInfo(@RequestParam String seriesName) {
        try {
            if (seriesName == null || seriesName.trim().isEmpty()) {
                return Result.error("seriesName 不能为空");
            }
            List<Map<String, Object>> rows = seriesMapper.selectSeriesInfoByName(seriesName.trim());
            if (rows == null) {
                rows = new ArrayList<>();
            }
            // 统一补充 variantCount 字段，便于前端卡片展示
            for (Map<String, Object> row : rows) {
                Object totalVariants = row.get("totalVariants");
                if (totalVariants == null) {
                    Object regular = row.get("regularVariants");
                    Object hidden = row.get("hiddenVariants");
                    int r = regular == null ? 0 : (regular instanceof Number ? ((Number) regular).intValue() : 0);
                    int h = hidden == null ? 0 : (hidden instanceof Number ? ((Number) hidden).intValue() : 0);
                    row.put("variantCount", r + h);
                } else {
                    row.put("variantCount", totalVariants);
                }
                row.put("isLimitedDesc", Boolean.TRUE.equals(toBool(row.get("isLimited"))) ? "限定款" : "常规款");
            }
            return Result.success(rows);
        } catch (Exception e) {
            log.warn("[AiTools] series/info 查询失败 seriesName={}: {}", seriesName, e.getMessage());
            return Result.error("查询系列信息失败: " + e.getMessage());
        }
    }

    /**
     * 3. 查系列款式列表
     * 用户问“XX系列有哪些款式”时调用
     */
    @GetMapping("/series/styles")
    public Result<List<Map<String, Object>>> querySeriesStyles(@RequestParam String seriesName) {
        try {
            if (seriesName == null || seriesName.trim().isEmpty()) {
                return Result.error("seriesName 不能为空");
            }
            List<Map<String, Object>> rows = saleVariantMapper.selectStylesBySeriesName(seriesName.trim());
            if (rows == null) {
                rows = new ArrayList<>();
            }
            // 标注是否隐藏款便于大模型描述
            for (Map<String, Object> row : rows) {
                Object isHidden = row.get("isHidden");
                row.put("isHiddenDesc", Boolean.TRUE.equals(toBool(isHidden)) ? "隐藏款" : "常规款");
            }
            return Result.success(rows);
        } catch (Exception e) {
            log.warn("[AiTools] series/styles 查询失败 seriesName={}: {}", seriesName, e.getMessage());
            return Result.error("查询系列款式失败: " + e.getMessage());
        }
    }

    /**
     * 3. 查用户订单列表
     * 用户问“我的订单/我买了什么”时调用
     */
    @GetMapping("/orders")
    public Result<List<Map<String, Object>>> queryUserOrders(@RequestParam String userId) {
        try {
            if (userId == null || userId.trim().isEmpty()) {
                return Result.error("userId 不能为空");
            }
            List<OrderListDTO> orders = orderService.getUserOrdersWithItems(userId, null);
            List<Map<String, Object>> result = new ArrayList<>();
            if (orders != null) {
                for (OrderListDTO o : orders) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("orderId", o.getOrderId());
                    item.put("orderNo", o.getOrderNo());
                    item.put("orderStatus", o.getOrderStatus());
                    item.put("actualAmount", o.getActualAmount());
                    item.put("totalQuantity", o.getTotalQuantity());
                    item.put("createTime", fmt(o.getCreateTime()));
                    item.put("paymentTime", fmt(o.getPaymentTime()));
                    item.put("shippedTime", fmt(o.getShippedTime()));
                    item.put("logisticsCompany", o.getLogisticsCompany());
                    item.put("trackingNumber", o.getTrackingNumber());
                    item.put("shopName", o.getShopName());
                    // 订单项简化为可读列表
                    List<Map<String, Object>> items = new ArrayList<>();
                    if (o.getItems() != null) {
                        for (OrderItem oi : o.getItems()) {
                            Map<String, Object> im = new LinkedHashMap<>();
                            im.put("productName", oi.getProductName());
                            im.put("productSpec", oi.getProductSpec());
                            im.put("quantity", oi.getQuantity());
                            im.put("unitPrice", oi.getUnitPrice());
                            items.add(im);
                        }
                    }
                    item.put("items", items);
                    result.add(item);
                }
            }
            return Result.success(result);
        } catch (Exception e) {
            log.warn("[AiTools] orders 查询失败 userId={}: {}", userId, e.getMessage());
            return Result.error("查询订单失败: " + e.getMessage());
        }
    }

    /**
     * 4. 查用户收藏列表
     * 用户问“我的收藏/我收藏了什么”时调用
     */
    @GetMapping("/favorites")
    public Result<List<Map<String, Object>>> queryUserFavorites(@RequestParam String userId) {
        try {
            if (userId == null || userId.trim().isEmpty()) {
                return Result.error("userId 不能为空");
            }
            List<Map<String, Object>> rows = userProductFavoriteMapper.selectFavoritesWithProduct(userId.trim());
            return Result.success(rows != null ? rows : new ArrayList<>());
        } catch (Exception e) {
            log.warn("[AiTools] favorites 查询失败 userId={}: {}", userId, e.getMessage());
            return Result.error("查询收藏失败: " + e.getMessage());
        }
    }

    /**
     * 5. 查用户抽盒历史
     * 用户问“我抽过哪些/我的抽盒记录”时调用
     */
    @GetMapping("/draw-history")
    public Result<List<Map<String, Object>>> queryUserDrawHistory(@RequestParam String userId) {
        try {
            if (userId == null || userId.trim().isEmpty()) {
                return Result.error("userId 不能为空");
            }
            List<BlindBoxDrawRecord> records = blindBoxDrawRecordMapper.selectUserRecords(userId.trim());
            List<Map<String, Object>> result = new ArrayList<>();
            if (records != null) {
                for (BlindBoxDrawRecord r : records) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("recordId", r.getRecordId());
                    m.put("machineName", r.getMachineName());
                    m.put("variantName", r.getVariantName());
                    m.put("drawType", r.getDrawType());
                    m.put("isHidden", Boolean.TRUE.equals(r.getIsHidden()));
                    m.put("isHiddenDesc", Boolean.TRUE.equals(r.getIsHidden()) ? "隐藏款" : "常规款");
                    m.put("isGuaranteed", Boolean.TRUE.equals(r.getIsGuaranteed()));
                    m.put("drawPrice", r.getDrawPrice());
                    m.put("status", r.getStatus());
                    m.put("orderNo", r.getOrderNo());
                    m.put("createdAt", fmt(r.getCreatedAt()));
                    result.add(m);
                }
            }
            return Result.success(result);
        } catch (Exception e) {
            log.warn("[AiTools] draw-history 查询失败 userId={}: {}", userId, e.getMessage());
            return Result.error("查询抽盒历史失败: " + e.getMessage());
        }
    }

    /**
     * 6. 查款式库存
     * 用户问“还有货吗/XX款有库存吗”时调用
     */
    @GetMapping("/style-stock")
    public Result<List<Map<String, Object>>> queryStyleStock(@RequestParam String styleName) {
        try {
            if (styleName == null || styleName.trim().isEmpty()) {
                return Result.error("styleName 不能为空");
            }
            List<Map<String, Object>> rows = saleVariantMapper.selectStyleStockByName(styleName.trim());
            if (rows == null) {
                rows = new ArrayList<>();
            }
            for (Map<String, Object> row : rows) {
                Object isHidden = row.get("isHidden");
                row.put("isHiddenDesc", Boolean.TRUE.equals(toBool(isHidden)) ? "隐藏款" : "常规款");
            }
            return Result.success(rows);
        } catch (Exception e) {
            log.warn("[AiTools] style-stock 查询失败 styleName={}: {}", styleName, e.getMessage());
            return Result.error("查询库存失败: " + e.getMessage());
        }
    }

    // ============= 工具方法 =============
    private static String fmt(Date date) {
        if (date == null) return null;
        synchronized (DATE_FMT) {
            return DATE_FMT.format(date);
        }
    }

    private static String fmt(java.time.LocalDateTime dt) {
        if (dt == null) return null;
        return dt.toString();
    }

    private static boolean toBool(Object v) {
        if (v == null) return false;
        if (v instanceof Boolean) return (Boolean) v;
        if (v instanceof Number) return ((Number) v).intValue() != 0;
        return "1".equals(String.valueOf(v)) || "true".equalsIgnoreCase(String.valueOf(v));
    }
}
