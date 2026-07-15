package com.example.trendytoysocialecommercehd.controller;

import com.example.trendytoysocialecommercehd.common.Result;
import com.example.trendytoysocialecommercehd.dto.MerchantLoginDTO;
import com.example.trendytoysocialecommercehd.dto.MerchantRegisterDTO;
import com.example.trendytoysocialecommercehd.dto.ShopWithStatsDTO;
import com.example.trendytoysocialecommercehd.entity.SaleVariant;
import com.example.trendytoysocialecommercehd.entity.Shop;
import com.example.trendytoysocialecommercehd.entity.ShopAdmin;
import com.example.trendytoysocialecommercehd.mapper.OrderItemMapper;
import com.example.trendytoysocialecommercehd.mapper.OrderMapper;
import com.example.trendytoysocialecommercehd.mapper.SaleVariantMapper;
import com.example.trendytoysocialecommercehd.service.ShopAdminService;
import com.example.trendytoysocialecommercehd.service.ShopService;
import com.example.trendytoysocialecommercehd.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.trendytoysocialecommercehd.dto.DashboardDataDTO;
import com.example.trendytoysocialecommercehd.entity.Order;
import com.example.trendytoysocialecommercehd.entity.OrderItem;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/api/merchant")
public class MerchantController {

    @Autowired
    private ShopAdminService shopAdminService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ShopService shopService;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private SaleVariantMapper saleVariantMapper;

    @PostMapping("/login")
    public Result<?> login(@RequestBody MerchantLoginDTO loginDTO) {
        try {
            ShopAdmin shopAdmin = shopAdminService.login(loginDTO);
            String token = jwtUtil.generateToken(shopAdmin.getAdminId());

            Map<String, Object> result = new HashMap<>();
            result.put("user", shopAdmin);
            result.put("token", token);

            return Result.success(result);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/register")
    public Result<?> register(@RequestBody MerchantRegisterDTO registerDTO) {
        try {
            ShopAdmin shopAdmin = shopAdminService.registerByPhone(registerDTO.getUsername(), registerDTO.getPassword());
            String token = jwtUtil.generateToken(shopAdmin.getAdminId());

            Map<String, Object> result = new HashMap<>();
            result.put("user", shopAdmin);
            result.put("token", token);
            return Result.success(result);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/info/current")
    public Result<?> getCurrentMerchantInfo(@RequestHeader("Authorization") String token) {
        try {
            String cleanToken = token.replace("Bearer ", "");

            if (!jwtUtil.validateToken(cleanToken)) {
                return Result.error("无效的token");
            }

            String adminId = jwtUtil.getUserIdFromToken(cleanToken);
            ShopAdmin shopAdmin = shopAdminService.getShopAdminById(adminId);
            return Result.success(shopAdmin);
        } catch (Exception e) {
            return Result.error("获取商户信息失败");
        }
    }

    @PostMapping("/apply")
    public Result<?> apply(@RequestBody Shop shop, @RequestHeader("Authorization") String token) {
        try {
            String cleanToken = token.replace("Bearer ", "");
            String adminId = jwtUtil.getUserIdFromToken(cleanToken);

            shop.setShopId("shop_" + System.currentTimeMillis());
            shop.setAuditStatus("待审核");
            shop.setShopStatus("待营业");
            shop.setAuditRound(1);
            shopService.createShop(shop);

            // 关联店铺和商家账号
            shopAdminService.updateShopId(adminId, shop.getShopId());

            return Result.success("申请提交成功");
        } catch (Exception e) {
            return Result.error("申请提交失败: " + e.getMessage());
        }
    }

    @GetMapping("/shop/current")
    public Result<?> getCurrentShop(@RequestHeader("Authorization") String token) {
        try {
            String cleanToken = token.replace("Bearer ", "");
            String adminId = jwtUtil.getUserIdFromToken(cleanToken);

            ShopAdmin shopAdmin = shopAdminService.getShopAdminById(adminId);
            if (shopAdmin.getShopId() == null || shopAdmin.getShopId().isEmpty()) {
                return Result.success(null);
            }

            Shop shop = shopService.getShopById(shopAdmin.getShopId());
            return Result.success(shop);
        } catch (Exception e) {
            return Result.error("获取店铺信息失败: " + e.getMessage());
        }
    }

    @PutMapping("/shop/update")
    public Result<?> updateShop(@RequestBody Shop shop, @RequestHeader("Authorization") String token) {
        try {
            String cleanToken = token.replace("Bearer ", "");
            String adminId = jwtUtil.getUserIdFromToken(cleanToken);

            ShopAdmin shopAdmin = shopAdminService.getShopAdminById(adminId);
            if (!shopAdmin.getShopId().equals(shop.getShopId())) {
                return Result.error("无权修改此店铺");
            }

            // 增加审核次数
            Shop existingShop = shopService.getShopById(shop.getShopId());
            shop.setAuditRound(existingShop.getAuditRound() + 1);
            shop.setAuditStatus("待审核");
            shopService.updateShop(shop.getShopId(), shop);

            return Result.success("更新成功");
        } catch (Exception e) {
            return Result.error("更新失败: " + e.getMessage());
        }
    }

    @GetMapping("/dashboard")
    public Result<?> getDashboardData(@RequestHeader("Authorization") String token) {
        try {
            String cleanToken = token.replace("Bearer ", "");
            String adminId = jwtUtil.getUserIdFromToken(cleanToken);
            ShopAdmin shopAdmin = shopAdminService.getShopAdminById(adminId);
            String shopId = shopAdmin.getShopId();

            if (shopId == null || shopId.isEmpty()) {
                return Result.error("请先申请店铺");
            }

            DashboardDataDTO dashboardData = new DashboardDataDTO();

            // 1. 获取今日销售额和订单量
            LocalDate today = LocalDate.now();
            LocalDateTime startOfDay = today.atStartOfDay();
            LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();

            List<Order> todayOrdersList = orderMapper.selectList(
                    new LambdaQueryWrapper<Order>()
                            .between(Order::getCreateTime, startOfDay, endOfDay)
            );

            // 筛选该店铺的订单（通过 order_item 关联）
            List<String> orderIds = new ArrayList<>();
            BigDecimal todaySales = BigDecimal.ZERO;
            int todayOrdersCount = 0;

            for (Order order : todayOrdersList) {
                List<OrderItem> items = orderItemMapper.selectList(
                        new LambdaQueryWrapper<OrderItem>()
                                .eq(OrderItem::getOrderId, order.getOrderId())
                                .eq(OrderItem::getItemSellerId, shopId)
                );
                if (!items.isEmpty()) {
                    orderIds.add(order.getOrderId());
                    todaySales = todaySales.add(order.getActualAmount());
                    todayOrdersCount++;
                }
            }
            dashboardData.setTodaySales(todaySales);
            dashboardData.setTodayOrders(todayOrdersCount);

            // 2. 待发货数
            int pendingShipmentCount = 0;
            List<Order> pendingOrders = orderMapper.selectList(
                    new LambdaQueryWrapper<Order>()
                            .eq(Order::getOrderStatus, "PENDING_SHIPMENT")
            );
            for (Order order : pendingOrders) {
                List<OrderItem> items = orderItemMapper.selectList(
                        new LambdaQueryWrapper<OrderItem>()
                                .eq(OrderItem::getOrderId, order.getOrderId())
                                .eq(OrderItem::getItemSellerId, shopId)
                );
                if (!items.isEmpty()) {
                    pendingShipmentCount++;
                }
            }
            dashboardData.setPendingShipment(pendingShipmentCount);

            // 3. 售后中数
            int afterSalesCount = 0;
            List<Order> afterSalesOrders = orderMapper.selectList(
                    new LambdaQueryWrapper<Order>()
                            .in(Order::getAfterSalesStatus, "PROCESSING", "IN_PROGRESS")
            );
            for (Order order : afterSalesOrders) {
                List<OrderItem> items = orderItemMapper.selectList(
                        new LambdaQueryWrapper<OrderItem>()
                                .eq(OrderItem::getOrderId, order.getOrderId())
                                .eq(OrderItem::getItemSellerId, shopId)
                );
                if (!items.isEmpty()) {
                    afterSalesCount++;
                }
            }
            dashboardData.setAfterSales(afterSalesCount);

            // 4. 商品总数
            int productCount = Math.toIntExact(saleVariantMapper.selectCount(
                    new LambdaQueryWrapper<SaleVariant>()
                            .eq(SaleVariant::getShopId, shopId)
            ));
            dashboardData.setProductCount(productCount);

            // 5. 近7天销售趋势
            List<DashboardDataDTO.SalesTrendItem> salesTrend = new ArrayList<>();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");
            for (int i = 6; i >= 0; i--) {
                LocalDate date = today.minusDays(i);
                LocalDateTime start = date.atStartOfDay();
                LocalDateTime end = date.plusDays(1).atStartOfDay();

                List<Order> orders = orderMapper.selectList(
                        new LambdaQueryWrapper<Order>()
                                .between(Order::getCreateTime, start, end)
                );

                BigDecimal daySales = BigDecimal.ZERO;
                for (Order order : orders) {
                    List<OrderItem> items = orderItemMapper.selectList(
                            new LambdaQueryWrapper<OrderItem>()
                                    .eq(OrderItem::getOrderId, order.getOrderId())
                                    .eq(OrderItem::getItemSellerId, shopId)
                    );
                    if (!items.isEmpty()) {
                        daySales = daySales.add(order.getActualAmount());
                    }
                }

                DashboardDataDTO.SalesTrendItem trendItem = new DashboardDataDTO.SalesTrendItem();
                trendItem.setDate(date.format(formatter));
                trendItem.setSales(daySales);
                salesTrend.add(trendItem);
            }
            dashboardData.setSalesTrend(salesTrend);

            // 6. 热门商品 TOP 5
            List<OrderItem> allItems = orderItemMapper.selectList(
                    new LambdaQueryWrapper<OrderItem>()
                            .eq(OrderItem::getItemSellerId, shopId)
            );

            Map<String, Integer> productSalesMap = new HashMap<>();
            Map<String, BigDecimal> productAmountMap = new HashMap<>();
            Map<String, String> productNameMap = new HashMap<>();
            Map<String, String> productImageMap = new HashMap<>();

            for (OrderItem item : allItems) {
                String productId = item.getProductId();
                productSalesMap.put(productId, productSalesMap.getOrDefault(productId, 0) + item.getQuantity());
                productAmountMap.put(productId, productAmountMap.getOrDefault(productId, BigDecimal.ZERO).add(item.getActualSubtotal()));
                productNameMap.put(productId, item.getProductName());
                productImageMap.put(productId, item.getProductImage());
            }

            List<Map.Entry<String, Integer>> sortedProducts = productSalesMap.entrySet().stream()
                    .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                    .limit(5)
                    .collect(Collectors.toList());

            List<DashboardDataDTO.HotProductItem> hotProducts = new ArrayList<>();
            int rank = 1;
            for (Map.Entry<String, Integer> entry : sortedProducts) {
                DashboardDataDTO.HotProductItem productItem = new DashboardDataDTO.HotProductItem();
                productItem.setRank(rank);
                productItem.setName(productNameMap.getOrDefault(entry.getKey(), ""));
                productItem.setSales(entry.getValue());
                productItem.setAmount(productAmountMap.getOrDefault(entry.getKey(), BigDecimal.ZERO));
                productItem.setImage(productImageMap.getOrDefault(entry.getKey(), ""));
                hotProducts.add(productItem);
                rank++;
            }
            dashboardData.setHotProducts(hotProducts);

            // 7. 待处理事项
            List<DashboardDataDTO.TaskItem> tasks = new ArrayList<>();
            DashboardDataDTO.TaskItem task1 = new DashboardDataDTO.TaskItem();
            task1.setId(1);
            task1.setTitle("待发货订单");
            task1.setCount(pendingShipmentCount);
            task1.setType("warning");
            tasks.add(task1);

            DashboardDataDTO.TaskItem task2 = new DashboardDataDTO.TaskItem();
            task2.setId(2);
            task2.setTitle("售后申请");
            task2.setCount(afterSalesCount);
            task2.setType("info");
            tasks.add(task2);

            // 库存不足商品（低于10件）
            int lowStockCount = Math.toIntExact(saleVariantMapper.selectCount(
                    new LambdaQueryWrapper<SaleVariant>()
                            .eq(SaleVariant::getShopId, shopId)
                            .lt(SaleVariant::getStockQuantity, 10)
            ));
            DashboardDataDTO.TaskItem task3 = new DashboardDataDTO.TaskItem();
            task3.setId(3);
            task3.setTitle("库存不足商品");
            task3.setCount(lowStockCount);
            task3.setType("warning");
            tasks.add(task3);

            dashboardData.setTasks(tasks);

            return Result.success(dashboardData);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取仪表盘数据失败: " + e.getMessage());
        }
    }

}