package com.example.trendytoysocialecommercehd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.trendytoysocialecommercehd.dto.BlindBoxMachineStatisticsDTO;
import com.example.trendytoysocialecommercehd.dto.BlindBoxPickRequestDTO;
import com.example.trendytoysocialecommercehd.dto.BlindBoxPickResultDTO;
import com.example.trendytoysocialecommercehd.dto.DrawRequestDTO;
import com.example.trendytoysocialecommercehd.dto.DrawResultDTO;
import com.example.trendytoysocialecommercehd.entity.*;
import com.example.trendytoysocialecommercehd.mapper.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class BlindBoxMachineService {

    @Autowired
    private BlindBoxMachineMapper blindBoxMachineMapper;

    @Autowired
    private BlindBoxDrawRecordMapper blindBoxDrawRecordMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private BlindBoxSlotMapper blindBoxSlotMapper;

    @Autowired
    private BlindBoxQueueMapper blindBoxQueueMapper;

    @Autowired
    private BlindBoxSetMapper blindBoxSetMapper;

    @Autowired
    private BlindBoxStorageService blindBoxStorageService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private com.example.trendytoysocialecommercehd.mapper.UserMapper userMapper;

    /**
     * 获取所有活跃的抽盒机列表
     */
    public List<BlindBoxMachine> getActiveMachines() {
        return blindBoxMachineMapper.selectActiveMachinesWithInfo();
    }

    /**
     * 获取抽盒机详情
     */
    public BlindBoxMachine getMachineDetail(String machineId) {
        BlindBoxMachine machine = blindBoxMachineMapper.selectMachineWithInfo(machineId);
        if (machine == null) {
            throw new RuntimeException("抽盒机不存在");
        }
        return machine;
    }

    /**
     * 获取抽盒机下的款式列表（独立模式：从图鉴 product 表查询，不复用 sale_variant）
     */
    public List<Product> getMachineVariants(String machineId) {
        BlindBoxMachine machine = blindBoxMachineMapper.selectById(machineId);
        if (machine == null) {
            throw new RuntimeException("抽盒机不存在");
        }
        if (machine.getSeriesId() == null || machine.getSeriesId().isEmpty()) {
            return new ArrayList<>();
        }
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getSeriesId, machine.getSeriesId())
               .orderByAsc(Product::getSeriesOrder);
        return productMapper.selectList(wrapper);
    }

    /**
     * 获取用户在某个抽盒机的抽盒历史
     */
    public List<BlindBoxDrawRecord> getUserDrawHistory(String machineId, String userId) {
        LambdaQueryWrapper<BlindBoxDrawRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BlindBoxDrawRecord::getMachineId, machineId)
               .eq(BlindBoxDrawRecord::getUserId, userId)
               .orderByDesc(BlindBoxDrawRecord::getCreatedAt);
        return blindBoxDrawRecordMapper.selectList(wrapper);
    }

    /**
     * 抽盒核心逻辑（独立模式：从可售盒位中随机抽取，支付成功后盒位状态变为已售出）
     */
    @Transactional(rollbackFor = Exception.class)
    public DrawResultDTO draw(DrawRequestDTO request) {
        BlindBoxMachine machine = blindBoxMachineMapper.selectById(request.getMachineId());
        if (machine == null) {
            throw new RuntimeException("抽盒机不存在");
        }
        if (!"ACTIVE".equals(machine.getMachineStatus())) {
            throw new RuntimeException("抽盒机已停用");
        }

        int drawCount = "TEN".equals(request.getDrawType()) ? 10 : 1;
        BigDecimal drawPrice = "TEN".equals(request.getDrawType())
                ? (machine.getTenDrawPrice() != null ? machine.getTenDrawPrice() : machine.getDrawPrice().multiply(new BigDecimal("10")))
                : machine.getDrawPrice();

        // 独立模式：查询所有可售盒位（AVAILABLE 状态的 slot）
        LambdaQueryWrapper<BlindBoxSlot> slotWrapper = new LambdaQueryWrapper<>();
        slotWrapper.eq(BlindBoxSlot::getMachineId, request.getMachineId())
                   .eq(BlindBoxSlot::getStatus, "AVAILABLE");
        List<BlindBoxSlot> availableSlots = blindBoxSlotMapper.selectList(slotWrapper);

        if (availableSlots.isEmpty()) {
            throw new RuntimeException("该抽盒机已售罄");
        }

        // 检查可售盒位是否足够
        if (availableSlots.size() < drawCount) {
            throw new RuntimeException("库存不足，当前仅剩 " + availableSlots.size() + " 个");
        }

        // 检查保底机制
        int userNonHiddenDraws = blindBoxDrawRecordMapper.countUserNonHiddenDraws(
                request.getMachineId(), request.getUserId());
        boolean shouldGuarantee = false;
        if (machine.getGuaranteeDraws() > 0 && userNonHiddenDraws >= machine.getGuaranteeDraws() - 1) {
            shouldGuarantee = true;
        }

        // 打乱可售盒位顺序
        List<BlindBoxSlot> tempAvailable = new ArrayList<>(availableSlots);
        Collections.shuffle(tempAvailable);

        // 随机抽取盒位
        List<DrawResultDTO.DrawnItem> drawnItems = new ArrayList<>();
        List<BlindBoxSlot> pickedSlots = new ArrayList<>();
        List<BlindBoxDrawRecord> records = new ArrayList<>();

        for (int i = 0; i < drawCount; i++) {
            BlindBoxSlot pickedSlot;

            // 最后一次抽取时检查保底（优先抽隐藏款盒位）
            if (shouldGuarantee && i == drawCount - 1) {
                pickedSlot = drawGuaranteedSlot(tempAvailable);
            } else {
                pickedSlot = tempAvailable.get(0);
            }
            tempAvailable.remove(pickedSlot);
            pickedSlots.add(pickedSlot);

            // 独立模式：使用 slot 缓存的款式信息
            boolean isHidden = Boolean.TRUE.equals(pickedSlot.getIsHidden());
            String variantId = pickedSlot.getVariantId();
            String variantName = pickedSlot.getVariantName() != null ? pickedSlot.getVariantName() : "未知款式";
            String variantImage = pickedSlot.getVariantImage() != null ? pickedSlot.getVariantImage() : "";

            DrawResultDTO.DrawnItem item = new DrawResultDTO.DrawnItem();
            item.setVariantId(variantId);
            item.setVariantName(variantName);
            item.setVariantImage(variantImage);
            item.setIsHidden(isHidden);
            item.setIsGuaranteed(shouldGuarantee && i == drawCount - 1);
            item.setPrice(machine.getDrawPrice());
            drawnItems.add(item);

            // 记录抽盒记录
            BlindBoxDrawRecord record = new BlindBoxDrawRecord();
            record.setRecordId(UUID.randomUUID().toString());
            record.setMachineId(request.getMachineId());
            record.setUserId(request.getUserId());
            record.setSetId(pickedSlot.getSetId());
            record.setSlotNo(pickedSlot.getSlotNo());
            record.setVariantId(variantId);
            record.setDrawType(request.getDrawType());
            record.setIsHidden(isHidden);
            record.setIsGuaranteed(shouldGuarantee && i == drawCount - 1);
            record.setDrawPrice(machine.getDrawPrice());
            record.setStatus("PENDING_OPEN");
            blindBoxDrawRecordMapper.insert(record);
            records.add(record);
        }

        // 独立模式：更新盒位状态为已售
        for (BlindBoxSlot slot : pickedSlots) {
            slot.setStatus("SOLD");
            slot.setDrawnBy(request.getUserId());
            slot.setDrawnAt(new Date());
            blindBoxSlotMapper.updateById(slot);
            // 更新套盒已售数
            if (slot.getSetId() != null) {
                blindBoxSetMapper.incrementSoldCount(slot.getSetId());
            }
        }

        // 更新抽盒机统计
        machine.setTotalDraws(machine.getTotalDraws() + drawCount);
        machine.setTotalStock(Math.max(0, machine.getTotalStock() - drawCount));
        blindBoxMachineMapper.updateById(machine);

        // 创建订单
        BigDecimal totalPrice = drawPrice;
        String orderId = createDrawOrder(machine, drawnItems, request, totalPrice);

        // 回填抽盒记录的订单ID
        Order drawOrder = orderService.getOrderById(orderId);
        String orderNo = drawOrder != null ? drawOrder.getOrderNo() : null;
        for (BlindBoxDrawRecord rec : records) {
            rec.setOrderId(orderId);
            rec.setOrderNo(orderNo);
            blindBoxDrawRecordMapper.updateById(rec);
        }

        DrawResultDTO result = new DrawResultDTO();
        result.setOrderId(orderId);
        result.setTotalPrice(totalPrice);
        result.setDrawnItems(drawnItems);

        return result;
    }

    /**
     * 保底抽取（优先抽隐藏款盒位）
     */
    private BlindBoxSlot drawGuaranteedSlot(List<BlindBoxSlot> slots) {
        for (BlindBoxSlot slot : slots) {
            if (Boolean.TRUE.equals(slot.getIsHidden())) {
                return slot;
            }
        }
        // 没有隐藏款则随机抽取第一个
        return slots.get(0);
    }

    /**
     * 创建抽盒订单（独立模式：按 variantId 分组）
     */
    private String createDrawOrder(BlindBoxMachine machine, List<DrawResultDTO.DrawnItem> drawnItems,
                                    DrawRequestDTO request, BigDecimal totalPrice) {
        List<com.example.trendytoysocialecommercehd.dto.OrderItemRequest> orderItems = new ArrayList<>();

        // 按款式分组（同一款式可能抽中多次）
        Map<String, List<DrawResultDTO.DrawnItem>> groupedItems = drawnItems.stream()
                .collect(Collectors.groupingBy(d -> d.getVariantId() != null ? d.getVariantId() : UUID.randomUUID().toString()));

        for (Map.Entry<String, List<DrawResultDTO.DrawnItem>> entry : groupedItems.entrySet()) {
            List<DrawResultDTO.DrawnItem> items = entry.getValue();
            DrawResultDTO.DrawnItem firstItem = items.get(0);

            com.example.trendytoysocialecommercehd.dto.OrderItemRequest orderItem = new com.example.trendytoysocialecommercehd.dto.OrderItemRequest();
            orderItem.setProductId(entry.getKey());
            orderItem.setOriginalPrice(firstItem.getPrice());
            orderItem.setUnitPrice(firstItem.getPrice());
            orderItem.setQuantity(items.size());
            orderItem.setSubtotalAmount(firstItem.getPrice().multiply(new BigDecimal(items.size())));
            orderItem.setAllocatedDiscount(BigDecimal.ZERO);
            orderItem.setActualSubtotal(firstItem.getPrice().multiply(new BigDecimal(items.size())));
            orderItem.setItemSellerId(machine.getShopId());
            orderItem.setProductName(firstItem.getVariantName());
            orderItem.setProductImage(firstItem.getVariantImage());

            // 设置规格信息
            String spec = machine.getMachineName();
            orderItem.setProductSpec(spec);

            orderItems.add(orderItem);
        }

        com.example.trendytoysocialecommercehd.dto.CreateOrderRequest orderRequest = new com.example.trendytoysocialecommercehd.dto.CreateOrderRequest();
        orderRequest.setUserId(request.getUserId());
        orderRequest.setAmount(totalPrice);
        orderRequest.setShippingFee(BigDecimal.ZERO);
        orderRequest.setTotalDiscount(BigDecimal.ZERO);
        orderRequest.setActualAmount(totalPrice);
        orderRequest.setAddressId(request.getAddressId());
        orderRequest.setUserRemark("抽盒机订单");
        orderRequest.setItems(orderItems);

        Order order = orderService.createOrder(orderRequest);
        return order.getOrderId();
    }

    /**
     * 创建抽盒机（独立模式：不复用商城数据，直接引用图鉴系列）
     * 流程：选择图鉴系列 → 设置单抽价格 → 配置套数/隐藏款数 → 生成所有盒子 → 入库
     * 盒子生成后款式固定，用户抽盒只能从可售盒子中随机抽取，支付成功后盒子状态变为已售出
     */
    @Transactional(rollbackFor = Exception.class)
    public BlindBoxMachine createMachine(BlindBoxMachine machine) {
        if (machine.getMachineId() == null || machine.getMachineId().isEmpty()) {
            machine.setMachineId(UUID.randomUUID().toString());
        }

        // 必填字段校验：优先使用 seriesId（新独立模式）
        if (machine.getSeriesId() == null || machine.getSeriesId().isEmpty()) {
            throw new RuntimeException("请选择图鉴系列");
        }
        if (machine.getMachineName() == null || machine.getMachineName().isEmpty()) {
            throw new RuntimeException("抽盒机名称不能为空");
        }
        if (machine.getSetCount() == null || machine.getSetCount() <= 0) {
            throw new RuntimeException("套数必须大于0");
        }
        if (machine.getHiddenCount() == null || machine.getHiddenCount() < 0) {
            machine.setHiddenCount(0);
        }

        // 创建后自动提交审核：审核状态=待审核，运行状态=停用（审核通过后自动启用）
        if (machine.getMachineStatus() == null || machine.getMachineStatus().isEmpty()) {
            machine.setMachineStatus("INACTIVE");
        }
        if (machine.getAuditStatus() == null || machine.getAuditStatus().isEmpty()) {
            machine.setAuditStatus("PENDING");
        }

        if (machine.getTotalDraws() == null) {
            machine.setTotalDraws(0);
        }
        if (machine.getTotalRevenue() == null) {
            machine.setTotalRevenue(BigDecimal.ZERO);
        }

        // 查询图鉴系列下的所有款式（从 product 表，不复用 sale_variant）
        LambdaQueryWrapper<Product> productWrapper = new LambdaQueryWrapper<>();
        productWrapper.eq(Product::getSeriesId, machine.getSeriesId());
        List<Product> allProducts = productMapper.selectList(productWrapper);

        if (allProducts.isEmpty()) {
            throw new RuntimeException("该图鉴系列下没有款式，无法创建抽盒机");
        }

        // 区分普通款和隐藏款
        List<Product> regularProducts = new ArrayList<>();
        List<Product> hiddenProducts = new ArrayList<>();
        for (Product p : allProducts) {
            if (Boolean.TRUE.equals(p.getHiddenVariant())) {
                hiddenProducts.add(p);
            } else {
                regularProducts.add(p);
            }
        }
        if (regularProducts.isEmpty()) {
            throw new RuntimeException("该系列没有普通款，无法生成套盒");
        }

        // 校验隐藏款数量不超过总盒数
        int totalBoxCount = machine.getSetCount() * regularProducts.size();
        if (machine.getHiddenCount() > totalBoxCount) {
            throw new RuntimeException("隐藏款数量(" + machine.getHiddenCount()
                    + ")不能超过总盒数(" + totalBoxCount + ")");
        }
        if (machine.getHiddenCount() > 0 && hiddenProducts.isEmpty()) {
            throw new RuntimeException("该系列没有隐藏款式，无法配置隐藏款");
        }

        // 总库存 = 总盒数（每个盒子是一个独立库存单位）
        machine.setTotalStock(totalBoxCount);

        blindBoxMachineMapper.insert(machine);

        // 生成所有套盒和盒子（款式已固定，打乱顺序后分配到各套的盒位图中）
        generateBoxesAtCreation(machine, regularProducts, hiddenProducts);

        return machine;
    }

    /**
     * 创建抽盒机时生成所有盒子（核心生成逻辑）
     * 规则：
     *   1. 每套包含该系列所有普通款各1个
     *   2. hiddenCount 个隐藏款随机分配到所有套盒中，每个隐藏款替换掉一个普通款
     *   3. 隐藏款概率 = hiddenCount / 总盒数（系统自动计算）
     *   4. 打乱顺序后分配到各套的盒位图
     *   5. 每个盒子缓存款式信息（name/image/type），不再依赖运行时查询
     */
    private void generateBoxesAtCreation(BlindBoxMachine machine,
                                          List<Product> regularProducts,
                                          List<Product> hiddenProducts) {
        int setCount = machine.getSetCount();
        int hiddenCount = machine.getHiddenCount();
        int regularCount = regularProducts.size();

        // 网格布局：3行，列数根据普通款数量决定（≤9款→3x3，>9款→3x4）
        int rows = 3;
        int cols = regularCount <= 9 ? 3 : 4;

        // 收集所有套盒的所有盒位（用于随机分配隐藏款）
        // 每个元素: [setIndex, positionInSet, originalProduct]
        List<int[]> allPositions = new ArrayList<>();
        for (int s = 0; s < setCount; s++) {
            for (int p = 0; p < regularCount; p++) {
                allPositions.add(new int[]{s, p});
            }
        }

        // 随机选择 hiddenCount 个位置放置隐藏款
        Collections.shuffle(allPositions);
        Set<String> hiddenPositionKeys = new HashSet<>();
        for (int i = 0; i < hiddenCount; i++) {
            int[] pos = allPositions.get(i);
            hiddenPositionKeys.add(pos[0] + "_" + pos[1]);
        }

        // 为每个套盒生成盒子
        for (int s = 0; s < setCount; s++) {
            // 构建该套的盒子列表（普通款 + 可能被替换为隐藏款）
            List<Product> setProducts = new ArrayList<>();
            for (int p = 0; p < regularCount; p++) {
                String key = s + "_" + p;
                if (hiddenPositionKeys.contains(key) && !hiddenProducts.isEmpty()) {
                    // 该位置放置隐藏款（随机选一个隐藏款式）
                    Product hiddenP = hiddenProducts.get(
                            new Random().nextInt(hiddenProducts.size()));
                    setProducts.add(hiddenP);
                } else {
                    setProducts.add(regularProducts.get(p));
                }
            }

            // 打乱顺序后分配到盒位图
            Collections.shuffle(setProducts);

            // 创建套盒记录
            BlindBoxSet set = new BlindBoxSet();
            set.setSetId(UUID.randomUUID().toString());
            set.setMachineId(machine.getMachineId());
            set.setSetIndex(s);
            set.setSetName("第" + (s + 1) + "套");
            set.setLayoutImage(null);
            set.setGridRows(rows);
            set.setGridCols(cols);
            set.setTotalSlots(regularCount);
            set.setSoldCount(0);
            set.setStatus("ACTIVE");
            blindBoxSetMapper.insert(set);

            // 创建盒位（slots）
            for (int i = 0; i < setProducts.size(); i++) {
                Product assigned = setProducts.get(i);
                boolean isHidden = Boolean.TRUE.equals(assigned.getHiddenVariant());

                BlindBoxSlot slot = new BlindBoxSlot();
                slot.setSlotId(UUID.randomUUID().toString());
                slot.setMachineId(machine.getMachineId());
                slot.setSetId(set.getSetId());
                slot.setSlotNo(i + 1);
                slot.setSlotCode("SLOT-" + (1000 + i + 1));
                slot.setStatus("AVAILABLE");
                slot.setVariantId(assigned.getProductId());
                // 缓存款式信息，不再依赖运行时查询 sale_variant
                slot.setVariantName(assigned.getName());
                slot.setVariantImage(assigned.getImageUrl());
                slot.setVariantType(isHidden ? "hidden" : "regular");
                slot.setIsHidden(isHidden);
                blindBoxSlotMapper.insert(slot);
            }
        }
    }

    /**
     * 更新抽盒机
     */
    public BlindBoxMachine updateMachine(String machineId, BlindBoxMachine machine) {
        machine.setMachineId(machineId);
        blindBoxMachineMapper.updateById(machine);
        return machine;
    }

    /**
     * 删除抽盒机
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteMachine(String machineId) {
        return blindBoxMachineMapper.deleteById(machineId) > 0;
    }

    // ==================== 选盒（Pick-box）相关方法 ====================

    /**
     * 获取抽盒机的所有套盒（含格位信息）
     * 独立模式：盒子在创建抽盒机时已全部生成，此处仅查询展示
     */
    @Transactional(rollbackFor = Exception.class)
    public List<BlindBoxSet> getMachineSets(String machineId) {
        BlindBoxMachine machine = blindBoxMachineMapper.selectById(machineId);
        if (machine == null) {
            throw new RuntimeException("抽盒机不存在");
        }

        List<BlindBoxSet> sets = blindBoxSetMapper.selectByMachineId(machineId);

        // 为每个活跃套盒填充格位信息
        for (BlindBoxSet set : sets) {
            if ("ACTIVE".equals(set.getStatus())) {
                List<BlindBoxSlot> slots = blindBoxSlotMapper.selectBySetId(set.getSetId());
                populateSlotDisplayInfo(slots);
                set.setSlots(slots);
            }
        }

        return sets;
    }

    /**
     * 获取指定套盒详情（含格位）
     */
    @Transactional(rollbackFor = Exception.class)
    public BlindBoxSet getSetDetail(String setId) {
        BlindBoxSet set = blindBoxSetMapper.selectById(setId);
        if (set == null) {
            throw new RuntimeException("套盒不存在");
        }
        List<BlindBoxSlot> slots = blindBoxSlotMapper.selectBySetId(setId);
        populateSlotDisplayInfo(slots);
        set.setSlots(slots);
        return set;
    }

    /**
     * 填充格位展示信息（独立模式：使用创建时缓存的款式信息，不再查询 sale_variant）：
     * - SOLD → 保留缓存的款式名称/图片用于展示
     * - AVAILABLE → 隐藏款式信息（防偷看），但保留盒位编号
     */
    private void populateSlotDisplayInfo(List<BlindBoxSlot> slots) {
        if (slots == null || slots.isEmpty()) return;

        for (BlindBoxSlot slot : slots) {
            if ("SOLD".equals(slot.getStatus())) {
                // 已售：保留缓存的款式信息用于展示（variantName/variantImage 已在创建时写入）
            } else {
                // AVAILABLE：隐藏款式信息（防偷看），保留盒位编号
                slot.setVariantId(null);
                slot.setVariantName(null);
                slot.setVariantImage(null);
                slot.setVariantType(null);
                slot.setIsHidden(null);
            }
        }
    }

    /**
     * 获取九宫格选盒状态（独立模式：盒子在创建时已全部生成）
     * 对未售出的槽位，隐藏款式信息（保持神秘感）
     * 已售出的槽位，保留缓存的款式信息用于展示
     */
    @Transactional(rollbackFor = Exception.class)
    public List<BlindBoxSlot> getMachineSlots(String machineId) {
        BlindBoxMachine machine = blindBoxMachineMapper.selectById(machineId);
        if (machine == null) {
            throw new RuntimeException("抽盒机不存在");
        }

        List<BlindBoxSlot> existingSlots = blindBoxSlotMapper.selectByMachineId(machineId);

        // 对未售出的槽位，隐藏款式信息（保持神秘感）
        // 已售出的槽位，保留缓存的款式信息
        for (BlindBoxSlot slot : existingSlots) {
            if (!"SOLD".equals(slot.getStatus())) {
                slot.setVariantId(null);
                slot.setVariantName(null);
                slot.setVariantImage(null);
                slot.setVariantType(null);
                slot.setIsHidden(null);
            }
        }

        return existingSlots;
    }

    /**
     * 加入排队
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> joinQueue(String machineId, String userId) {
        BlindBoxMachine machine = blindBoxMachineMapper.selectById(machineId);
        if (machine == null) {
            throw new RuntimeException("抽盒机不存在");
        }

        // 检查是否已在队列中
        BlindBoxQueue existing = blindBoxQueueMapper.selectUserActiveQueue(machineId, userId);
        if (existing != null) {
            // 已在队列中，返回当前位置
            Map<String, Object> result = new HashMap<>();
            result.put("queuePosition", existing.getQueuePosition());
            result.put("queueCount", blindBoxQueueMapper.countActiveQueue(machineId));
            result.put("status", existing.getStatus());
            result.put("canPick", "ACTIVE".equals(existing.getStatus()));
            return result;
        }

        // 检查当前是否有人正在抽盒（ACTIVE 状态）
        List<BlindBoxQueue> activeQueue = blindBoxQueueMapper.selectActiveQueue(machineId);
        boolean hasActiveUser = activeQueue.stream().anyMatch(q -> "ACTIVE".equals(q.getStatus()));

        // 创建排队记录
        int position = blindBoxQueueMapper.countActiveQueue(machineId) + 1;
        BlindBoxQueue queue = new BlindBoxQueue();
        queue.setQueueId(UUID.randomUUID().toString());
        queue.setMachineId(machineId);
        queue.setUserId(userId);
        queue.setQueuePosition(position);
        // 只有队列中没有人时，第一个才为 ACTIVE
        queue.setStatus(!hasActiveUser && position == 1 ? "ACTIVE" : "WAITING");

        blindBoxQueueMapper.insert(queue);

        Map<String, Object> result = new HashMap<>();
        result.put("queuePosition", position);
        result.put("queueCount", position);
        result.put("status", queue.getStatus());
        result.put("canPick", "ACTIVE".equals(queue.getStatus()));
        return result;
    }

    /**
     * 离开排队
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean leaveQueue(String machineId, String userId) {
        int rows = blindBoxQueueMapper.leaveQueue(machineId, userId);
        if (rows > 0) {
            // 将队首的 WAITING 用户激活
            List<BlindBoxQueue> activeQueue = blindBoxQueueMapper.selectActiveQueue(machineId);
            if (!activeQueue.isEmpty()) {
                BlindBoxQueue first = activeQueue.get(0);
                if ("WAITING".equals(first.getStatus())) {
                    blindBoxQueueMapper.activateUser(first.getQueueId());
                }
            }
        }
        return rows > 0;
    }

    /**
     * 查询用户排队状态
     */
    public Map<String, Object> getQueueStatus(String machineId, String userId) {
        Map<String, Object> result = new HashMap<>();
        BlindBoxQueue userQueue = blindBoxQueueMapper.selectUserActiveQueue(machineId, userId);
        int queueCount = blindBoxQueueMapper.countActiveQueue(machineId);

        // 查询当前活跃用户（正在选盒的人）
        List<BlindBoxQueue> activeQueue = blindBoxQueueMapper.selectActiveQueue(machineId);
        BlindBoxQueue activeUser = activeQueue.stream()
                .filter(q -> "ACTIVE".equals(q.getStatus()))
                .findFirst().orElse(null);

        if (activeUser != null) {
            result.put("activeUserId", activeUser.getUserId());
            // 查询活跃用户名
            try {
                User activeUserEntity = userMapper.selectById(activeUser.getUserId());
                if (activeUserEntity != null) {
                    result.put("activeUsername", activeUserEntity.getUsername());
                }
            } catch (Exception e) {
                log.warn("查询活跃用户信息失败: {}", e.getMessage());
            }
        }

        if (userQueue == null) {
            // 不在队列中
            result.put("inQueue", false);
            result.put("status", "NONE");
            result.put("queuePosition", 0);
            result.put("queueCount", queueCount);
            // 没人正在选盒时可以直接抽
            result.put("canPick", activeUser == null);
        } else {
            result.put("inQueue", true);
            result.put("status", userQueue.getStatus()); // ACTIVE / WAITING
            result.put("queuePosition", userQueue.getQueuePosition());
            result.put("queueCount", queueCount);
            result.put("canPick", "ACTIVE".equals(userQueue.getStatus()));
        }
        return result;
    }

    /**
     * 选盒购买（选中某个盒子立即购买并揭晓）
     * 独立模式：盒子款式已固定在 slot 中，支付成功后盒子状态变为已售出
     * 不再扣减 sale_variant 库存，使用 slot 缓存的款式信息
     */
    @Transactional(rollbackFor = Exception.class)
    public BlindBoxPickResultDTO pickBlindBox(BlindBoxPickRequestDTO request) {
        BlindBoxMachine machine = blindBoxMachineMapper.selectById(request.getMachineId());
        if (machine == null) {
            throw new RuntimeException("抽盒机不存在");
        }
        if (!"ACTIVE".equals(machine.getMachineStatus())) {
            throw new RuntimeException("抽盒机已停用");
        }

        // 校验排队状态：只有 ACTIVE 用户或无排队时才能抽盒
        if (request.getUserId() != null && !request.getUserId().isEmpty()) {
            BlindBoxQueue userQueue = blindBoxQueueMapper.selectUserActiveQueue(request.getMachineId(), request.getUserId());
            int queueCount = blindBoxQueueMapper.countActiveQueue(request.getMachineId());
            if (queueCount > 0) {
                if (userQueue == null) {
                    throw new RuntimeException("请先加入排队");
                }
                if (!"ACTIVE".equals(userQueue.getStatus())) {
                    throw new RuntimeException("前方还有" + (userQueue.getQueuePosition() - 1) + "人排队，请耐心等待");
                }
            }
        }

        // 查找选中的槽位（优先按 setId 查找，兼容旧数据按 machineId 查找）
        LambdaQueryWrapper<BlindBoxSlot> slotWrapper = new LambdaQueryWrapper<>();
        if (request.getSetId() != null && !request.getSetId().isEmpty()) {
            slotWrapper.eq(BlindBoxSlot::getSetId, request.getSetId())
                       .eq(BlindBoxSlot::getSlotNo, request.getSlotNo());
        } else {
            slotWrapper.eq(BlindBoxSlot::getMachineId, request.getMachineId())
                       .eq(BlindBoxSlot::getSlotNo, request.getSlotNo());
        }
        BlindBoxSlot slot = blindBoxSlotMapper.selectOne(slotWrapper);

        if (slot == null) {
            throw new RuntimeException("槽位不存在");
        }
        if ("SOLD".equals(slot.getStatus())) {
            throw new RuntimeException("该盒子已被抽走，请选择其他盒子");
        }

        // 独立模式：直接使用 slot 缓存的款式信息，不再查询 sale_variant
        boolean isHidden = Boolean.TRUE.equals(slot.getIsHidden());
        String variantId = slot.getVariantId();
        String variantName = slot.getVariantName() != null ? slot.getVariantName() : "未知款式";
        String variantImage = slot.getVariantImage() != null ? slot.getVariantImage() : "";

        // 检查保底机制
        int userNonHiddenDraws = blindBoxDrawRecordMapper.countUserNonHiddenDraws(
                request.getMachineId(), request.getUserId());
        boolean isGuaranteed = machine.getGuaranteeDraws() > 0
                && userNonHiddenDraws >= machine.getGuaranteeDraws() - 1
                && !isHidden;

        // 独立模式：不再扣减 sale_variant 库存，仅更新槽位状态为已售
        slot.setStatus("SOLD");
        slot.setDrawnBy(request.getUserId());
        slot.setDrawnAt(new Date());
        blindBoxSlotMapper.updateById(slot);

        // 更新套盒已售数（如果有关联套盒）
        if (slot.getSetId() != null) {
            blindBoxSetMapper.incrementSoldCount(slot.getSetId());
        }

        // 更新抽盒机统计
        machine.setTotalDraws(machine.getTotalDraws() + 1);
        machine.setTotalStock(Math.max(0, machine.getTotalStock() - 1));
        blindBoxMachineMapper.updateById(machine);

        // 记录抽盒历史
        BlindBoxDrawRecord record = new BlindBoxDrawRecord();
        record.setRecordId(UUID.randomUUID().toString());
        record.setMachineId(request.getMachineId());
        record.setUserId(request.getUserId());
        record.setSetId(slot.getSetId());
        record.setSlotNo(slot.getSlotNo());
        record.setVariantId(variantId);
        record.setDrawType("PICK");
        record.setIsHidden(isHidden);
        record.setIsGuaranteed(isGuaranteed);
        record.setDrawPrice(machine.getDrawPrice());
        record.setStatus("PENDING_OPEN");
        blindBoxDrawRecordMapper.insert(record);

        // 创建订单（作为支付凭证）
        String orderId = createPickOrder(machine, variantId, variantName, variantImage,
                request, machine.getDrawPrice(), isHidden);

        // 回填抽盒记录的订单ID
        Order pickOrder = orderService.getOrderById(orderId);
        record.setOrderId(orderId);
        record.setOrderNo(pickOrder != null ? pickOrder.getOrderNo() : null);
        blindBoxDrawRecordMapper.updateById(record);

        // 抽中后自动存入暂存柜
        BlindBoxStorage storage = blindBoxStorageService.storeToCabinet(
                request.getUserId(),
                request.getMachineId(),
                slot.getSetId(),
                slot.getSlotNo(),
                variantId,
                isHidden,
                machine.getDrawPrice(),
                orderId
        );

        // 构建返回结果（使用 slot 缓存的款式信息）
        BlindBoxPickResultDTO result = new BlindBoxPickResultDTO();
        Order order = orderService.getOrderById(orderId);
        result.setOrderId(orderId);
        result.setOrderNo(order != null ? order.getOrderNo() : "");
        result.setSlotNo(slot.getSlotNo());
        result.setSlotCode(slot.getSlotCode());
        result.setVariantId(variantId);
        result.setVariantName(variantName);
        result.setVariantImage(variantImage);
        result.setIsHidden(isHidden);
        result.setIsGuaranteed(isGuaranteed);
        result.setPrice(machine.getDrawPrice());
        result.setTotalPrice(machine.getDrawPrice());
        result.setStorageId(storage.getStorageId());

        return result;
    }

    /**
     * 创建选盒订单（独立模式：使用 slot 缓存的款式信息，不再依赖 sale_variant）
     */
    private String createPickOrder(BlindBoxMachine machine, String variantId, String variantName,
                                    String variantImage, BlindBoxPickRequestDTO request,
                                    BigDecimal price, boolean isHidden) {
        List<com.example.trendytoysocialecommercehd.dto.OrderItemRequest> orderItems = new ArrayList<>();

        com.example.trendytoysocialecommercehd.dto.OrderItemRequest orderItem = new com.example.trendytoysocialecommercehd.dto.OrderItemRequest();
        orderItem.setProductId(variantId != null ? variantId : UUID.randomUUID().toString());
        orderItem.setOriginalPrice(price);
        orderItem.setUnitPrice(price);
        orderItem.setQuantity(1);
        orderItem.setSubtotalAmount(price);
        orderItem.setAllocatedDiscount(BigDecimal.ZERO);
        orderItem.setActualSubtotal(price);
        orderItem.setItemSellerId(machine.getShopId());
        orderItem.setProductName(variantName);
        orderItem.setProductImage(variantImage);
        orderItem.setProductSpec(machine.getMachineName());
        orderItems.add(orderItem);

        com.example.trendytoysocialecommercehd.dto.CreateOrderRequest orderRequest = new com.example.trendytoysocialecommercehd.dto.CreateOrderRequest();
        orderRequest.setUserId(request.getUserId());
        orderRequest.setAmount(price);
        orderRequest.setShippingFee(BigDecimal.ZERO);
        orderRequest.setTotalDiscount(BigDecimal.ZERO);
        orderRequest.setActualAmount(price);
        orderRequest.setAddressId(request.getAddressId());
        orderRequest.setUserRemark("选盒购买订单");
        orderRequest.setItems(orderItems);

        Order order = orderService.createOrder(orderRequest);
        return order.getOrderId();
    }

    // ==================== 商家端方法 ====================

    /**
     * 商家端：查询某店铺下的抽盒机列表（含筛选）
     */
    public List<BlindBoxMachine> getMerchantMachines(String shopId, String keyword,
                                                      String machineStatus, String auditStatus) {
        return blindBoxMachineMapper.selectMerchantMachinesWithInfo(shopId, keyword, machineStatus, auditStatus);
    }

    /**
     * 商家端：根据 machineId 和 shopId 双重校验获取抽盒机（防止越权）
     */
    public BlindBoxMachine getMerchantMachine(String machineId, String shopId) {
        BlindBoxMachine machine = blindBoxMachineMapper.selectMachineWithInfo(machineId);
        if (machine == null) {
            throw new RuntimeException("抽盒机不存在");
        }
        if (shopId != null && !shopId.equals(machine.getShopId())) {
            throw new RuntimeException("无权操作他人抽盒机");
        }
        return machine;
    }

    /**
     * 商家端：获取抽盒机款式配置（独立模式：从图鉴 product 表查询）
     * 返回该抽盒机关联系列下的所有款式信息
     */
    public List<Map<String, Object>> getMachineVariantsConfig(String machineId) {
        BlindBoxMachine machine = blindBoxMachineMapper.selectById(machineId);
        if (machine == null) {
            throw new RuntimeException("抽盒机不存在");
        }

        // 独立模式：必须关联图鉴系列
        String seriesId = machine.getSeriesId();
        if (seriesId == null || seriesId.isEmpty()) {
            return new ArrayList<>();
        }

        // 从图鉴 product 表查询款式信息
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getSeriesId, seriesId)
               .orderByAsc(Product::getSeriesOrder);
        List<Product> products = productMapper.selectList(wrapper);

        // 统计每个款式在 slot 中的可售数量
        LambdaQueryWrapper<BlindBoxSlot> slotWrapper = new LambdaQueryWrapper<>();
        slotWrapper.eq(BlindBoxSlot::getMachineId, machineId)
                   .eq(BlindBoxSlot::getStatus, "AVAILABLE");
        List<BlindBoxSlot> availableSlots = blindBoxSlotMapper.selectList(slotWrapper);
        Map<String, Integer> stockMap = new HashMap<>();
        for (BlindBoxSlot slot : availableSlots) {
            if (slot.getVariantId() != null) {
                stockMap.merge(slot.getVariantId(), 1, Integer::sum);
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Product p : products) {
            Map<String, Object> v = new HashMap<>();
            v.put("machineId", machineId);
            v.put("variantId", p.getProductId());
            v.put("variantName", p.getName());
            v.put("variantImage", p.getImageUrl());
            v.put("isHidden", Boolean.TRUE.equals(p.getHiddenVariant()));
            v.put("availableStock", stockMap.getOrDefault(p.getProductId(), 0));
            result.add(v);
        }
        return result;
    }

    /**
     * 商家端：更新抽盒机状态（启用/下架）
     * 商家可控制 ACTIVE/TAKEDOWN 状态，不能设置 INACTIVE（禁用由管理员控制）
     * 状态约束：审核未通过的抽盒机不能启用
     */
    @Transactional(rollbackFor = Exception.class)
    public BlindBoxMachine updateMachineStatus(String machineId, String shopId, String status) {
        BlindBoxMachine machine = getMerchantMachine(machineId, shopId);
        if ("ACTIVE".equals(status) && !"APPROVED".equals(machine.getAuditStatus())) {
            throw new RuntimeException("审核未通过的抽盒机不能启用");
        }
        if ("INACTIVE".equals(status)) {
            throw new RuntimeException("商家无权设置禁用状态，禁用由管理员控制");
        }
        machine.setMachineStatus(status);
        blindBoxMachineMapper.updateById(machine);
        return machine;
    }

    /**
     * 商家端：提交审核
     * 状态约束：草稿或驳回状态可提交，已通过/待审核状态不可重复提交
     */
    @Transactional(rollbackFor = Exception.class)
    public BlindBoxMachine submitForAudit(String machineId, String shopId) {
        BlindBoxMachine machine = getMerchantMachine(machineId, shopId);
        String audit = machine.getAuditStatus();
        if (!"DRAFT".equals(audit) && !"REJECTED".equals(audit)) {
            throw new RuntimeException("当前审核状态不允许提交审核");
        }
        machine.setAuditStatus("PENDING");
        machine.setAuditRemark(null);
        blindBoxMachineMapper.updateById(machine);
        return machine;
    }

    /**
     * 商家端：获取抽盒机统计数据
     */
    public BlindBoxMachineStatisticsDTO getMachineStatistics(String machineId, String shopId) {
        // 商家端校验越权
        getMerchantMachine(machineId, shopId);

        return buildStatistics(machineId);
    }

    /**
     * 商家端/管理员端：获取抽盒机抽盒记录（分页）
     */
    public List<BlindBoxDrawRecord> getMachineRecords(String machineId, String userId, String drawType) {
        return blindBoxDrawRecordMapper.selectMachineRecords(machineId, userId, drawType);
    }

    /**
     * 获取用户所有抽盒记录
     */
    public List<BlindBoxDrawRecord> getUserAllDrawRecords(String userId) {
        return blindBoxDrawRecordMapper.selectUserRecords(userId);
    }

    /**
     * 获取用户在某台抽盒机的抽盒记录
     */
    public List<BlindBoxDrawRecord> getUserMachineDrawRecords(String userId, String machineId) {
        return blindBoxDrawRecordMapper.selectMachineRecords(machineId, userId, null);
    }

    /**
     * 开盒：更新记录状态为已开盒
     */
    @Transactional(rollbackFor = Exception.class)
    public BlindBoxDrawRecord openBox(String recordId, String userId) {
        BlindBoxDrawRecord record = blindBoxDrawRecordMapper.selectById(recordId);
        if (record == null) {
            throw new RuntimeException("抽盒记录不存在");
        }
        if (!record.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作");
        }
        if (!"PENDING_OPEN".equals(record.getStatus())) {
            throw new RuntimeException("该记录已开盒");
        }
        record.setStatus("OPENED");
        record.setOpenedAt(new Date());
        blindBoxDrawRecordMapper.updateById(record);
        return record;
    }

    /**
     * 欧气排行榜（按隐藏款数量降序）
     */
    public List<Map<String, Object>> getLuckRanking(int limit) {
        return blindBoxDrawRecordMapper.selectLuckRanking(limit);
    }

    // ==================== 管理员端方法 ====================

    /**
     * 管理员端：全平台抽盒机列表（含筛选）
     */
    public List<BlindBoxMachine> getAllMachines(String shopId, String machineStatus,
                                                 String auditStatus, String keyword) {
        return blindBoxMachineMapper.selectAllMachinesWithInfo(shopId, machineStatus, auditStatus, keyword);
    }

    /**
     * 管理员端：审核通过
     */
    @Transactional(rollbackFor = Exception.class)
    public BlindBoxMachine approveMachine(String machineId) {
        BlindBoxMachine machine = blindBoxMachineMapper.selectById(machineId);
        if (machine == null) {
            throw new RuntimeException("抽盒机不存在");
        }
        if (!"PENDING".equals(machine.getAuditStatus())) {
            throw new RuntimeException("仅待审核状态可执行审核操作");
        }
        machine.setAuditStatus("APPROVED");
        machine.setMachineStatus("ACTIVE");
        machine.setAuditedAt(new Date());
        blindBoxMachineMapper.updateById(machine);
        return machine;
    }

    /**
     * 管理员端：审核驳回
     */
    @Transactional(rollbackFor = Exception.class)
    public BlindBoxMachine rejectMachine(String machineId, String remark) {
        BlindBoxMachine machine = blindBoxMachineMapper.selectById(machineId);
        if (machine == null) {
            throw new RuntimeException("抽盒机不存在");
        }
        if (!"PENDING".equals(machine.getAuditStatus())) {
            throw new RuntimeException("仅待审核状态可执行审核操作");
        }
        if (remark == null || remark.trim().isEmpty()) {
            throw new RuntimeException("驳回原因不能为空");
        }
        machine.setAuditStatus("REJECTED");
        machine.setAuditRemark(remark);
        machine.setAuditedAt(new Date());
        // 驳回后自动停用
        machine.setMachineStatus("INACTIVE");
        blindBoxMachineMapper.updateById(machine);
        return machine;
    }

    /**
     * 管理员端：强制下架违规抽盒机
     */
    @Transactional(rollbackFor = Exception.class)
    public BlindBoxMachine takedownMachine(String machineId, String reason) {
        BlindBoxMachine machine = blindBoxMachineMapper.selectById(machineId);
        if (machine == null) {
            throw new RuntimeException("抽盒机不存在");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new RuntimeException("下架原因不能为空");
        }
        machine.setMachineStatus("TAKEDOWN");
        machine.setAuditRemark(reason);
        blindBoxMachineMapper.updateById(machine);
        return machine;
    }

    /**
     * 管理员端：启用/禁用抽盒机
     * 管理员可控制 ACTIVE/INACTIVE 状态，不能设置 TAKEDOWN
     */
    @Transactional(rollbackFor = Exception.class)
    public BlindBoxMachine adminUpdateMachineStatus(String machineId, String status) {
        BlindBoxMachine machine = blindBoxMachineMapper.selectById(machineId);
        if (machine == null) {
            throw new RuntimeException("抽盒机不存在");
        }
        if ("ACTIVE".equals(status) && !"APPROVED".equals(machine.getAuditStatus())) {
            throw new RuntimeException("审核未通过的抽盒机不能启用");
        }
        if ("TAKEDOWN".equals(status)) {
            throw new RuntimeException("管理员无权设置下架状态，请使用强制下架接口");
        }
        machine.setMachineStatus(status);
        blindBoxMachineMapper.updateById(machine);
        return machine;
    }

    /**
     * 管理员端：获取抽盒机统计数据
     */
    public BlindBoxMachineStatisticsDTO getAdminMachineStatistics(String machineId) {
        BlindBoxMachine machine = blindBoxMachineMapper.selectById(machineId);
        if (machine == null) {
            throw new RuntimeException("抽盒机不存在");
        }
        return buildStatistics(machineId);
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 重新计算抽盒机总库存（独立模式：统计可售盒位数量）
     */
    private void recalcMachineTotalStock(BlindBoxMachine machine) {
        int availableCount = blindBoxSlotMapper.countAvailableSlots(machine.getMachineId());
        machine.setTotalStock(availableCount);
    }

    /**
     * 构建抽盒机统计数据
     */
    private BlindBoxMachineStatisticsDTO buildStatistics(String machineId) {
        BlindBoxMachine machine = blindBoxMachineMapper.selectById(machineId);
        if (machine == null) {
            throw new RuntimeException("抽盒机不存在");
        }

        BlindBoxMachineStatisticsDTO dto = new BlindBoxMachineStatisticsDTO();
        dto.setMachineId(machineId);
        dto.setMachineName(machine.getMachineName());

        // 总抽数与流水优先用抽盒机冗余字段（性能好），无则实时统计
        int totalDraws = machine.getTotalDraws() != null ? machine.getTotalDraws()
                : blindBoxDrawRecordMapper.countByMachineId(machineId);
        BigDecimal totalRevenue = machine.getTotalRevenue() != null ? machine.getTotalRevenue()
                : blindBoxDrawRecordMapper.sumRevenueByMachineId(machineId);
        dto.setTotalDraws(totalDraws);
        dto.setTotalRevenue(totalRevenue);
        dto.setUniqueUsers(blindBoxDrawRecordMapper.countDistinctUsers(machineId));
        dto.setGuaranteedDraws(blindBoxDrawRecordMapper.countGuaranteedDraws(machineId));

        // 款式统计
        List<Map<String, Object>> rows = blindBoxDrawRecordMapper.countVariantDrawStats(machineId);
        List<BlindBoxMachineStatisticsDTO.VariantDrawStat> variantStats = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            BlindBoxMachineStatisticsDTO.VariantDrawStat stat =
                    new BlindBoxMachineStatisticsDTO.VariantDrawStat();
            stat.setVariantName((String) row.get("variantName"));
            stat.setVariantImage((String) row.get("variantImage"));
            Object hiddenObj = row.get("isHidden");
            stat.setIsHidden(hiddenObj != null && ((Number) hiddenObj).intValue() == 1);
            Number drawCount = (Number) row.get("drawCount");
            int count = drawCount != null ? drawCount.intValue() : 0;
            stat.setDrawCount(count);
            if (totalDraws > 0) {
                stat.setDrawRatio(new BigDecimal(count).divide(new BigDecimal(totalDraws), 4, BigDecimal.ROUND_HALF_UP));
            } else {
                stat.setDrawRatio(BigDecimal.ZERO);
            }
            variantStats.add(stat);
        }
        dto.setVariantStats(variantStats);
        return dto;
    }
}
