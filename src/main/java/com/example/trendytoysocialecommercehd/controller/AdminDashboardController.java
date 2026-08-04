package com.example.trendytoysocialecommercehd.controller;

import com.example.trendytoysocialecommercehd.common.Result;
import com.example.trendytoysocialecommercehd.dto.AdminDashboardDTO;
import com.example.trendytoysocialecommercehd.entity.User;
import com.example.trendytoysocialecommercehd.entity.Order;
import com.example.trendytoysocialecommercehd.entity.Shop;
import com.example.trendytoysocialecommercehd.entity.OrderItem;
import com.example.trendytoysocialecommercehd.entity.SocialActivity;
import com.example.trendytoysocialecommercehd.entity.MyDisplayCabinet;
import com.example.trendytoysocialecommercehd.entity.Comment;
import com.example.trendytoysocialecommercehd.entity.FollowRelationship;
import com.example.trendytoysocialecommercehd.entity.SaleVariant;
import com.example.trendytoysocialecommercehd.entity.Report;
import com.example.trendytoysocialecommercehd.entity.AfterSale;
import com.example.trendytoysocialecommercehd.mapper.UserMapper;
import com.example.trendytoysocialecommercehd.mapper.ShopMapper;
import com.example.trendytoysocialecommercehd.mapper.OrderMapper;
import com.example.trendytoysocialecommercehd.mapper.OrderItemMapper;
import com.example.trendytoysocialecommercehd.mapper.SocialActivityMapper;
import com.example.trendytoysocialecommercehd.mapper.MyDisplayCabinetMapper;
import com.example.trendytoysocialecommercehd.mapper.CommentMapper;
import com.example.trendytoysocialecommercehd.mapper.FollowRelationshipMapper;
import com.example.trendytoysocialecommercehd.mapper.SaleVariantMapper;
import com.example.trendytoysocialecommercehd.mapper.ReportMapper;
import com.example.trendytoysocialecommercehd.mapper.AfterSaleMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ShopMapper shopMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private SocialActivityMapper socialActivityMapper;

    @Autowired
    private MyDisplayCabinetMapper myDisplayCabinetMapper;

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private FollowRelationshipMapper followRelationshipMapper;

    @Autowired
    private SaleVariantMapper saleVariantMapper;

    @Autowired
    private ReportMapper reportMapper;

    @Autowired
    private AfterSaleMapper afterSaleMapper;

    @GetMapping
    public Result<AdminDashboardDTO> getDashboardData(
            @RequestParam(name = "days", defaultValue = "7") Integer days) {
        try {
            AdminDashboardDTO dto = new AdminDashboardDTO();

            // 1. 核心指标 - 从数据库获取真实数据
            // 总用户数
            QueryWrapper<User> userCountWrapper = new QueryWrapper<>();
            long userCountResult = userMapper.selectCount(userCountWrapper);
            dto.setTotalUsers((int) userCountResult);

            // 今日新增用户
            Date todayStart = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
            Date tomorrowStart = Date.from(LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
            QueryWrapper<User> todayUserWrapper = new QueryWrapper<>();
            todayUserWrapper.ge("register_time", todayStart);
            todayUserWrapper.lt("register_time", tomorrowStart);
            long todayUserCountResult = userMapper.selectCount(todayUserWrapper);
            dto.setTodayNewUsers((int) todayUserCountResult);

            // 入驻商家数
            QueryWrapper<Shop> shopCountWrapper = new QueryWrapper<>();
            long shopCountResult = shopMapper.selectCount(shopCountWrapper);
            dto.setTotalMerchants((int) shopCountResult);

            // 总交易额 - 从orders表计算actual_amount总和
            QueryWrapper<Order> totalRevenueWrapper = new QueryWrapper<>();
            List<Order> allOrders = orderMapper.selectList(totalRevenueWrapper);
            BigDecimal totalRevenue = BigDecimal.ZERO;
            for (Order order : allOrders) {
                if (order.getActualAmount() != null) {
                    totalRevenue = totalRevenue.add(order.getActualAmount());
                }
            }
            dto.setTotalRevenue(totalRevenue);

            // 今日交易额 - 从orders表计算今日的actual_amount总和
            LocalDateTime todayBegin = LocalDate.now().atStartOfDay();
            LocalDateTime tomorrowBegin = todayBegin.plusDays(1);
            QueryWrapper<Order> todayRevenueWrapper = new QueryWrapper<>();
            todayRevenueWrapper.ge("create_time", todayBegin);
            todayRevenueWrapper.lt("create_time", tomorrowBegin);
            List<Order> todayOrders = orderMapper.selectList(todayRevenueWrapper);
            BigDecimal todayRevenue = BigDecimal.ZERO;
            for (Order order : todayOrders) {
                if (order.getActualAmount() != null) {
                    todayRevenue = todayRevenue.add(order.getActualAmount());
                }
            }
            dto.setTodayRevenue(todayRevenue);

            // 2. 销售趋势数据 - 完全从数据库获取
            List<AdminDashboardDTO.SalesTrendItem> salesTrend = new ArrayList<>();
            List<AdminDashboardDTO.OrderTrendItem> orderTrend = new ArrayList<>();
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MM-dd");

            for (int i = days - 1; i >= 0; i--) {
                LocalDate date = LocalDate.now().minusDays(i);
                LocalDateTime dayStart = date.atStartOfDay();
                LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();

                // 获取当天交易额
                QueryWrapper<Order> dayRevenueWrapper = new QueryWrapper<>();
                dayRevenueWrapper.ge("create_time", dayStart);
                dayRevenueWrapper.lt("create_time", dayEnd);
                List<Order> dayOrders = orderMapper.selectList(dayRevenueWrapper);
                BigDecimal dayRevenue = BigDecimal.ZERO;
                for (Order order : dayOrders) {
                    if (order.getActualAmount() != null) {
                        dayRevenue = dayRevenue.add(order.getActualAmount());
                    }
                }

                // 获取当天订单数
                int dayOrderCount = dayOrders.size();

                AdminDashboardDTO.SalesTrendItem salesItem = new AdminDashboardDTO.SalesTrendItem();
                salesItem.setDate(date.format(dateFormatter));
                salesItem.setSales(dayRevenue);
                salesTrend.add(salesItem);

                AdminDashboardDTO.OrderTrendItem orderItem = new AdminDashboardDTO.OrderTrendItem();
                orderItem.setDate(date.format(dateFormatter));
                orderItem.setOrders(dayOrderCount);
                orderTrend.add(orderItem);
            }
            dto.setSalesTrend(salesTrend);
            dto.setOrderTrend(orderTrend);

            // 3. 热门商品数据 - 从 order_items 表统计
            List<AdminDashboardDTO.HotProductItem> hotProducts = new ArrayList<>();
            QueryWrapper<OrderItem> orderItemWrapper = new QueryWrapper<>();
            // 只查询有商品名称的记录
            orderItemWrapper.isNotNull("product_name");
            List<OrderItem> allOrderItems = orderItemMapper.selectList(orderItemWrapper);

            // 按商品分组统计
            Map<String, List<OrderItem>> productMap = allOrderItems.stream()
                    .filter(item -> item.getProductId() != null)
                    .collect(Collectors.groupingBy(OrderItem::getProductId));

            // 统计每个商品的销售数量和销售金额
            List<Map.Entry<String, List<OrderItem>>> sortedProducts = productMap.entrySet().stream()
                    .sorted((a, b) -> {
                        int countA = a.getValue().stream().mapToInt(OrderItem::getQuantity).sum();
                        int countB = b.getValue().stream().mapToInt(OrderItem::getQuantity).sum();
                        return Integer.compare(countB, countA); // 按销量降序
                    })
                    .limit(5) // 取前5名
                    .collect(Collectors.toList());

            int rank = 1;
            for (Map.Entry<String, List<OrderItem>> entry : sortedProducts) {
                List<OrderItem> items = entry.getValue();
                int totalQuantity = items.stream().mapToInt(OrderItem::getQuantity).sum();
                BigDecimal totalAmount = BigDecimal.ZERO;
                for (OrderItem item : items) {
                    if (item.getActualSubtotal() != null) {
                        totalAmount = totalAmount.add(item.getActualSubtotal());
                    }
                }

                // 找到有商品名称的记录
                OrderItem representativeItem = items.stream()
                        .filter(item -> item.getProductName() != null && !item.getProductName().isEmpty())
                        .findFirst()
                        .orElse(items.get(0));

                AdminDashboardDTO.HotProductItem hotProduct = new AdminDashboardDTO.HotProductItem();
                hotProduct.setRank(rank++);
                hotProduct.setProductName(representativeItem.getProductName() != null ?
                        representativeItem.getProductName() : "商品" + entry.getKey().substring(0, 8));
                hotProduct.setProductImage(representativeItem.getProductImage());
                hotProduct.setSalesCount(totalQuantity);
                hotProduct.setTotalSales(totalAmount);
                hotProducts.add(hotProduct);
            }



            // 4. 商家相关指标 - 从数据库获取
            List<AdminDashboardDTO.MetricItem> merchantMetrics = new ArrayList<>();

            AdminDashboardDTO.MetricItem metric1 = new AdminDashboardDTO.MetricItem();
            metric1.setName("入驻商家总数");
            metric1.setValue(dto.getTotalMerchants());
            merchantMetrics.add(metric1);

            // 查询待审核商家数
            QueryWrapper<Shop> pendingShopWrapper = new QueryWrapper<>();
            pendingShopWrapper.eq("audit_status", "PENDING");
            long pendingShopCount = shopMapper.selectCount(pendingShopWrapper);
            AdminDashboardDTO.MetricItem metric2 = new AdminDashboardDTO.MetricItem();
            metric2.setName("待审核商家");
            metric2.setValue((int) pendingShopCount);
            if (pendingShopCount > 0) {
                metric2.setType("warning");
            }
            merchantMetrics.add(metric2);

            // 今日新增商家
            QueryWrapper<Shop> todayShopWrapper = new QueryWrapper<>();
            todayShopWrapper.ge("created_at", todayStart);
            todayShopWrapper.lt("created_at", tomorrowStart);
            long todayShopCount = shopMapper.selectCount(todayShopWrapper);
            AdminDashboardDTO.MetricItem metric3 = new AdminDashboardDTO.MetricItem();
            metric3.setName("今日新增商家");
            metric3.setValue((int) todayShopCount);
            merchantMetrics.add(metric3);

            // 商家商品总数 - 从 sale_variant 表统计
            QueryWrapper<SaleVariant> saleVariantCountWrapper = new QueryWrapper<>();
            long totalProductCount = saleVariantMapper.selectCount(saleVariantCountWrapper);
            AdminDashboardDTO.MetricItem metric4 = new AdminDashboardDTO.MetricItem();
            metric4.setName("商家商品总数");
            metric4.setValue((int) totalProductCount);
            merchantMetrics.add(metric4);

            dto.setMerchantMetrics(merchantMetrics);

            // 5. 用户/内容相关指标 - 从数据库获取
            List<AdminDashboardDTO.MetricItem> userContentMetrics = new ArrayList<>();

            // 社交活动发布总数
            QueryWrapper<SocialActivity> activityCountWrapper = new QueryWrapper<>();
            long activityCount = socialActivityMapper.selectCount(activityCountWrapper);
            AdminDashboardDTO.MetricItem userMetric1 = new AdminDashboardDTO.MetricItem();
            userMetric1.setName("社交活动发布总数");
            userMetric1.setValue((int) activityCount);
            userContentMetrics.add(userMetric1);

            // 待审核活动数
            QueryWrapper<SocialActivity> pendingActivityWrapper = new QueryWrapper<>();
            pendingActivityWrapper.eq("audit_status", "PENDING");
            long pendingActivityCount = socialActivityMapper.selectCount(pendingActivityWrapper);
            AdminDashboardDTO.MetricItem userMetric2 = new AdminDashboardDTO.MetricItem();
            userMetric2.setName("待审核活动数");
            userMetric2.setValue((int) pendingActivityCount);
            if (pendingActivityCount > 0) {
                userMetric2.setType("warning");
            }
            userContentMetrics.add(userMetric2);

            // 展示柜创建总数
            QueryWrapper<MyDisplayCabinet> cabinetCountWrapper = new QueryWrapper<>();
            long cabinetCount = myDisplayCabinetMapper.selectCount(cabinetCountWrapper);
            AdminDashboardDTO.MetricItem userMetric3 = new AdminDashboardDTO.MetricItem();
            userMetric3.setName("展示柜创建总数");
            userMetric3.setValue((int) cabinetCount);
            userContentMetrics.add(userMetric3);

            // 用户互动总数（点赞/评论）
            QueryWrapper<Comment> commentCountWrapper = new QueryWrapper<>();
            long commentCount = commentMapper.selectCount(commentCountWrapper);
            QueryWrapper<FollowRelationship> followCountWrapper = new QueryWrapper<>();
            long followCount = followRelationshipMapper.selectCount(followCountWrapper);
            List<SocialActivity> allActivities = socialActivityMapper.selectList(new QueryWrapper<>());
            int totalLikeCount = allActivities.stream()
                    .mapToInt(activity -> activity.getLikeCount() != null ? activity.getLikeCount() : 0)
                    .sum();
            long totalInteractions = commentCount + followCount + totalLikeCount;
            AdminDashboardDTO.MetricItem userMetric4 = new AdminDashboardDTO.MetricItem();
            userMetric4.setName("用户互动总数");
            userMetric4.setValue((int) totalInteractions);
            userContentMetrics.add(userMetric4);

            dto.setUserContentMetrics(userContentMetrics);

            // 6. 待审核事项 - 全部从数据库实时统计
            List<AdminDashboardDTO.PendingTaskItem> pendingTasks = new ArrayList<>();

            // 6.1 商家入驻申请待审核
            AdminDashboardDTO.PendingTaskItem task1 = new AdminDashboardDTO.PendingTaskItem();
            task1.setId(1);
            task1.setTitle("商家入驻申请待审核");
            task1.setType("merchant");
            task1.setCount((int) pendingShopCount);
            pendingTasks.add(task1);

            // 6.2 社交活动待审核
            AdminDashboardDTO.PendingTaskItem task2 = new AdminDashboardDTO.PendingTaskItem();
            task2.setId(2);
            task2.setTitle("社交活动待审核");
            task2.setType("activity");
            task2.setCount((int) pendingActivityCount);
            pendingTasks.add(task2);

            // 6.3 售后申请待处理（after_sale_status = 'PENDING'）
            QueryWrapper<AfterSale> pendingAfterSaleWrapper = new QueryWrapper<>();
            pendingAfterSaleWrapper.eq("after_sale_status", "PENDING");
            long pendingAfterSaleCount = afterSaleMapper.selectCount(pendingAfterSaleWrapper);
            AdminDashboardDTO.PendingTaskItem task3 = new AdminDashboardDTO.PendingTaskItem();
            task3.setId(3);
            task3.setTitle("售后申请待处理");
            task3.setType("afterSale");
            task3.setCount((int) pendingAfterSaleCount);
            pendingTasks.add(task3);

            // 6.4 举报待处理（status = 'PENDING'）
            QueryWrapper<Report> pendingReportWrapper = new QueryWrapper<>();
            pendingReportWrapper.eq("status", "PENDING");
            long pendingReportCount = reportMapper.selectCount(pendingReportWrapper);
            AdminDashboardDTO.PendingTaskItem task4 = new AdminDashboardDTO.PendingTaskItem();
            task4.setId(4);
            task4.setTitle("举报待处理");
            task4.setType("report");
            task4.setCount((int) pendingReportCount);
            pendingTasks.add(task4);

            dto.setPendingTasks(pendingTasks);

            // 7. 平台健康状态
            List<AdminDashboardDTO.HealthItem> healthStatus = new ArrayList<>();

            AdminDashboardDTO.HealthItem health1 = new AdminDashboardDTO.HealthItem();
            health1.setName("服务器状态");
            health1.setValue("正常");
            health1.setNormalRange("-");
            health1.setStatus("good");
            healthStatus.add(health1);

            AdminDashboardDTO.HealthItem health2 = new AdminDashboardDTO.HealthItem();
            health2.setName("API响应时间");
            health2.setValue("245ms");
            health2.setNormalRange("<500ms");
            health2.setStatus("good");
            healthStatus.add(health2);

            AdminDashboardDTO.HealthItem health3 = new AdminDashboardDTO.HealthItem();
            health3.setName("今日活跃用户");
            health3.setValue("1,234");
            health3.setNormalRange("-");
            health3.setStatus("-");
            healthStatus.add(health3);

            // 订单完成率
            QueryWrapper<Order> completedOrderWrapper = new QueryWrapper<>();
            completedOrderWrapper.eq("order_status", "COMPLETED");
            long completedOrderCount = orderMapper.selectCount(completedOrderWrapper);
            QueryWrapper<Order> allOrderWrapper = new QueryWrapper<>();
            long totalOrderCount = orderMapper.selectCount(allOrderWrapper);
            double orderCompletionRate = totalOrderCount > 0 ? (completedOrderCount * 100.0 / totalOrderCount) : 0;
            AdminDashboardDTO.HealthItem health4 = new AdminDashboardDTO.HealthItem();
            health4.setName("订单完成率");
            health4.setValue(String.format("%.1f%%", orderCompletionRate));
            health4.setNormalRange(">90%");
            health4.setStatus(orderCompletionRate >= 90 ? "good" : "warning");
            healthStatus.add(health4);

            AdminDashboardDTO.HealthItem health5 = new AdminDashboardDTO.HealthItem();
            health5.setName("平均发货时长");
            health5.setValue("6.2小时");
            health5.setNormalRange("<24小时");
            health5.setStatus("good");
            healthStatus.add(health5);

            AdminDashboardDTO.HealthItem health6 = new AdminDashboardDTO.HealthItem();
            health6.setName("用户投诉率");
            health6.setValue("1.2%");
            health6.setNormalRange("<3%");
            health6.setStatus("good");
            healthStatus.add(health6);

            dto.setHealthStatus(healthStatus);

            return Result.success(dto);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取仪表盘数据失败: " + e.getMessage());
        }
    }
}
