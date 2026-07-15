package com.example.trendytoysocialecommercehd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.trendytoysocialecommercehd.dto.BlindBoxMachineStatisticsDTO;
import com.example.trendytoysocialecommercehd.dto.BlindBoxPickRequestDTO;
import com.example.trendytoysocialecommercehd.dto.BlindBoxPickResultDTO;
import com.example.trendytoysocialecommercehd.dto.DrawRequestDTO;
import com.example.trendytoysocialecommercehd.dto.DrawResultDTO;
import com.example.trendytoysocialecommercehd.entity.*;
import com.example.trendytoysocialecommercehd.mapper.*;
import lombok.Data;
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
    private SaleVariantMapper saleVariantMapper;

    @Autowired
    private SaleSeriesMapper saleSeriesMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private BlindBoxSlotMapper blindBoxSlotMapper;

    @Autowired
    private BlindBoxQueueMapper blindBoxQueueMapper;

    @Autowired
    private BlindBoxMachineVariantMapper blindBoxMachineVariantMapper;

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
     * 获取抽盒机下的款式列表（含库存信息）
     */
    public List<SaleVariant> getMachineVariants(String machineId) {
        BlindBoxMachine machine = blindBoxMachineMapper.selectById(machineId);
        if (machine == null) {
            throw new RuntimeException("抽盒机不存在");
        }

        LambdaQueryWrapper<SaleVariant> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SaleVariant::getSaleSeriesId, machine.getSaleSeriesId())
               .eq(SaleVariant::getSaleStatus, "上架")
               .gt(SaleVariant::getStockQuantity, 0);
        return saleVariantMapper.selectList(wrapper);
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
     * 抽盒核心逻辑
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

        // 获取该系列下有库存的款式
        LambdaQueryWrapper<SaleVariant> variantWrapper = new LambdaQueryWrapper<>();
        variantWrapper.eq(SaleVariant::getSaleSeriesId, machine.getSaleSeriesId())
                      .eq(SaleVariant::getSaleStatus, "上架")
                      .gt(SaleVariant::getStockQuantity, 0);
        List<SaleVariant> availableVariants = saleVariantMapper.selectList(variantWrapper);

        if (availableVariants.isEmpty()) {
            throw new RuntimeException("该系列已售罄");
        }

        // 检查库存是否足够
        int totalAvailableStock = availableVariants.stream()
                .mapToInt(SaleVariant::getStockQuantity)
                .sum();
        if (totalAvailableStock < drawCount) {
            throw new RuntimeException("库存不足，当前仅剩 " + totalAvailableStock + " 个");
        }

        // 检查保底机制
        int userNonHiddenDraws = blindBoxDrawRecordMapper.countUserNonHiddenDraws(
                request.getMachineId(), request.getUserId());
        boolean shouldGuarantee = false;
        if (machine.getGuaranteeDraws() > 0 && userNonHiddenDraws >= machine.getGuaranteeDraws() - 1) {
            shouldGuarantee = true;
        }

        // 随机抽取款式
        List<DrawResultDTO.DrawnItem> drawnItems = new ArrayList<>();
        List<SaleVariant> tempAvailable = new ArrayList<>(availableVariants);

        for (int i = 0; i < drawCount; i++) {
            SaleVariant drawnVariant;

            // 最后一次抽取时检查保底
            if (shouldGuarantee && i == drawCount - 1) {
                drawnVariant = drawGuaranteedVariant(tempAvailable);
            } else {
                drawnVariant = drawRandomVariant(tempAvailable);
            }

            // 扣减库存
            drawnVariant.setStockQuantity(drawnVariant.getStockQuantity() - 1);
            saleVariantMapper.updateById(drawnVariant);

            // 从临时列表中移除无库存的款式
            if (drawnVariant.getStockQuantity() <= 0) {
                tempAvailable.remove(drawnVariant);
            }

            // 判断是否为隐藏款
            boolean isHidden = false;
            if (drawnVariant.getVariantId() != null) {
                Product product = productMapper.selectById(drawnVariant.getVariantId());
                if (product != null && product.getHiddenVariant() != null && product.getHiddenVariant()) {
                    isHidden = true;
                }
            }

            // 解析款式图片
            String variantImage = "";
            try {
                String images = drawnVariant.getCustomImages();
                if (images != null && images.startsWith("[")) {
                    String parsed = images.substring(1, images.length() - 1);
                    if (parsed.contains(",")) {
                        variantImage = parsed.split(",")[0].trim().replaceAll("\"", "");
                    } else {
                        variantImage = parsed.trim().replaceAll("\"", "");
                    }
                } else if (images != null) {
                    variantImage = images;
                }
            } catch (Exception e) {
                variantImage = "";
            }

            DrawResultDTO.DrawnItem item = new DrawResultDTO.DrawnItem();
            item.setSaleVariantId(drawnVariant.getSaleVariantId());
            item.setVariantId(drawnVariant.getVariantId());
            item.setVariantName(drawnVariant.getCustomDescription() != null
                    ? drawnVariant.getCustomDescription()
                    : drawnVariant.getSkuCode());
            item.setVariantImage(variantImage);
            item.setIsHidden(isHidden);
            item.setIsGuaranteed(shouldGuarantee && i == drawCount - 1);
            item.setPrice(drawnVariant.getSalePrice());
            drawnItems.add(item);

            // 记录抽盒记录
            BlindBoxDrawRecord record = new BlindBoxDrawRecord();
            record.setRecordId(UUID.randomUUID().toString());
            record.setMachineId(request.getMachineId());
            record.setUserId(request.getUserId());
            record.setSaleVariantId(drawnVariant.getSaleVariantId());
            record.setVariantId(drawnVariant.getVariantId());
            record.setDrawType(request.getDrawType());
            record.setIsHidden(isHidden);
            record.setIsGuaranteed(shouldGuarantee && i == drawCount - 1);
            record.setDrawPrice(drawnVariant.getSalePrice());
            record.setStatus("PENDING_OPEN");
            blindBoxDrawRecordMapper.insert(record);
        }

        // 更新抽盒机统计
        machine.setTotalDraws(machine.getTotalDraws() + drawCount);
        machine.setTotalStock(machine.getTotalStock() - drawCount);
        blindBoxMachineMapper.updateById(machine);

        // 创建订单
        BigDecimal totalPrice = drawPrice;
        String orderId = createDrawOrder(machine, drawnItems, request, totalPrice);

        // 回填抽盒记录的订单ID
        Order drawOrder = orderService.getOrderById(orderId);
        String orderNo = drawOrder != null ? drawOrder.getOrderNo() : null;
        for (DrawResultDTO.DrawnItem di : drawnItems) {
            LambdaQueryWrapper<BlindBoxDrawRecord> rw = new LambdaQueryWrapper<>();
            rw.eq(BlindBoxDrawRecord::getMachineId, request.getMachineId())
              .eq(BlindBoxDrawRecord::getUserId, request.getUserId())
              .eq(BlindBoxDrawRecord::getSaleVariantId, di.getSaleVariantId())
              .isNull(BlindBoxDrawRecord::getOrderId)
              .orderByDesc(BlindBoxDrawRecord::getCreatedAt)
              .last("LIMIT 1");
            BlindBoxDrawRecord rec = blindBoxDrawRecordMapper.selectOne(rw);
            if (rec != null) {
                rec.setOrderId(orderId);
                rec.setOrderNo(orderNo);
                blindBoxDrawRecordMapper.updateById(rec);
            }
        }

        DrawResultDTO result = new DrawResultDTO();
        result.setOrderId(orderId);
        result.setTotalPrice(totalPrice);
        result.setDrawnItems(drawnItems);

        return result;
    }

    /**
     * 随机抽取款式（按库存权重）
     */
    private SaleVariant drawRandomVariant(List<SaleVariant> variants) {
        int totalWeight = variants.stream().mapToInt(SaleVariant::getStockQuantity).sum();
        int random = new Random().nextInt(totalWeight);

        int cumulative = 0;
        for (SaleVariant variant : variants) {
            cumulative += variant.getStockQuantity();
            if (random < cumulative) {
                return variant;
            }
        }
        return variants.get(variants.size() - 1);
    }

    /**
     * 保底抽取（优先抽隐藏款）
     */
    private SaleVariant drawGuaranteedVariant(List<SaleVariant> variants) {
        // 查找隐藏款
        for (SaleVariant variant : variants) {
            if (variant.getVariantId() != null) {
                Product product = productMapper.selectById(variant.getVariantId());
                if (product != null && product.getHiddenVariant() != null && product.getHiddenVariant()) {
                    return variant;
                }
            }
        }
        // 没有隐藏款则随机抽取
        return drawRandomVariant(variants);
    }

    /**
     * 创建抽盒订单
     */
    private String createDrawOrder(BlindBoxMachine machine, List<DrawResultDTO.DrawnItem> drawnItems,
                                    DrawRequestDTO request, BigDecimal totalPrice) {
        List<com.example.trendytoysocialecommercehd.dto.OrderItemRequest> orderItems = new ArrayList<>();

        // 按款式分组（同一款式可能抽中多次）
        Map<String, List<DrawResultDTO.DrawnItem>> groupedItems = drawnItems.stream()
                .collect(Collectors.groupingBy(DrawResultDTO.DrawnItem::getSaleVariantId));

        for (Map.Entry<String, List<DrawResultDTO.DrawnItem>> entry : groupedItems.entrySet()) {
            List<DrawResultDTO.DrawnItem> items = entry.getValue();
            DrawResultDTO.DrawnItem firstItem = items.get(0);

            com.example.trendytoysocialecommercehd.dto.OrderItemRequest orderItem = new com.example.trendytoysocialecommercehd.dto.OrderItemRequest();
            orderItem.setProductId(firstItem.getSaleVariantId());
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
     * 创建抽盒机
     */
    public BlindBoxMachine createMachine(BlindBoxMachine machine) {
        if (machine.getMachineId() == null || machine.getMachineId().isEmpty()) {
            machine.setMachineId(UUID.randomUUID().toString());
        }

        // 必填字段校验
        if (machine.getSaleSeriesId() == null || machine.getSaleSeriesId().isEmpty()) {
            throw new RuntimeException("关联销售系列ID不能为空");
        }
        if (machine.getMachineName() == null || machine.getMachineName().isEmpty()) {
            throw new RuntimeException("抽盒机名称不能为空");
        }

        // 默认状态：草稿 + 停用
        if (machine.getMachineStatus() == null || machine.getMachineStatus().isEmpty()) {
            machine.setMachineStatus("INACTIVE");
        }
        if (machine.getAuditStatus() == null || machine.getAuditStatus().isEmpty()) {
            machine.setAuditStatus("DRAFT");
        }

        // 计算总库存（优先使用款式覆盖配置中的库存，未覆盖的款式使用 sale_variant 默认库存）
        recalcMachineTotalStock(machine);

        if (machine.getTotalDraws() == null) {
            machine.setTotalDraws(0);
        }
        if (machine.getTotalRevenue() == null) {
            machine.setTotalRevenue(BigDecimal.ZERO);
        }

        blindBoxMachineMapper.insert(machine);
        return machine;
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
     * 删除抽盒机（同时级联删除款式覆盖配置）
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteMachine(String machineId) {
        blindBoxMachineVariantMapper.deleteByMachineId(machineId);
        return blindBoxMachineMapper.deleteById(machineId) > 0;
    }

    // ==================== 选盒（Pick-box）相关方法 ====================

    /**
     * 获取抽盒机的所有套盒（含格位信息）
     * 如果没有套盒，自动生成 3 套（每套 9 格）
     */
    @Transactional(rollbackFor = Exception.class)
    public List<BlindBoxSet> getMachineSets(String machineId) {
        BlindBoxMachine machine = blindBoxMachineMapper.selectById(machineId);
        if (machine == null) {
            throw new RuntimeException("抽盒机不存在");
        }

        if (machine.getSaleSeriesId() == null || machine.getSaleSeriesId().isEmpty()) {
            throw new RuntimeException("该抽盒机未关联销售系列");
        }

        List<BlindBoxSet> sets = blindBoxSetMapper.selectByMachineId(machineId);

        // 如果没有套盒或活跃套盒为 0，自动生成
        if (sets.isEmpty() || blindBoxSetMapper.selectActiveByMachineId(machineId).isEmpty()) {
            int startIndex = blindBoxSetMapper.selectMaxSetIndex(machineId) + 1;
            int setsToCreate = Math.max(1, 3 - sets.size());
            for (int i = 0; i < setsToCreate; i++) {
                try {
                    BlindBoxSet newSet = createBoxSet(machineId, machine.getSaleSeriesId(), startIndex + i, null);
                    sets.add(newSet);
                } catch (Exception e) {
                    log.error("自动创建套盒失败: {}", e.getMessage());
                    break;
                }
            }
        }

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
     * 创建一个套盒（每个款式占一格，库存为0的款式显示已售出）
     * 格子数 = 该系列普通款总数（12款→3x4，9款→3x3）
     */
    @Transactional(rollbackFor = Exception.class)
    public BlindBoxSet createBoxSet(String machineId, String saleSeriesId, int setIndex, String layoutImage) {
        // 查询该系列所有上架款式
        LambdaQueryWrapper<SaleVariant> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SaleVariant::getSaleSeriesId, saleSeriesId)
               .eq(SaleVariant::getSaleStatus, "上架");
        List<SaleVariant> allVariants = saleVariantMapper.selectList(wrapper);

        if (allVariants.isEmpty()) {
            throw new RuntimeException("该系列没有上架款式，无法生成套盒");
        }

        // 区分普通款和隐藏款
        List<SaleVariant> normalVariants = new ArrayList<>();
        List<SaleVariant> hiddenVariants = new ArrayList<>();
        for (SaleVariant sv : allVariants) {
            boolean isHidden = false;
            if (sv.getVariantId() != null) {
                Product product = productMapper.selectById(sv.getVariantId());
                if (product != null && product.getHiddenVariant() != null && product.getHiddenVariant()) {
                    isHidden = true;
                }
            }
            if (isHidden) hiddenVariants.add(sv);
            else normalVariants.add(sv);
        }

        // 普通款总数决定格子布局：9款→3x3，12款→3x4，其他按 ceil(N/3) 列
        int normalCount = normalVariants.size();
        if (normalCount == 0) {
            // 没有普通款，全部是隐藏款，回退到 3x3
            normalVariants = allVariants;
            hiddenVariants.clear();
            normalCount = normalVariants.size();
        }
        int rows = 3;
        int cols;
        if (normalCount <= 9) {
            cols = 3; // 9款及以下 → 3x3
        } else {
            cols = 4; // 超过9款（如12款）→ 3x4
        }
        int totalSlots = normalCount;

        // 创建套盒
        BlindBoxSet set = new BlindBoxSet();
        set.setSetId(UUID.randomUUID().toString());
        set.setMachineId(machineId);
        set.setSetIndex(setIndex);
        set.setSetName("第" + (setIndex + 1) + "套");
        set.setLayoutImage(layoutImage);
        set.setGridRows(rows);
        set.setGridCols(cols);
        set.setTotalSlots(totalSlots);
        set.setSoldCount(0);
        set.setStatus("ACTIVE");
        blindBoxSetMapper.insert(set);

        // 打乱普通款顺序，分配到格位
        Collections.shuffle(normalVariants);
        int soldCount = 0;
        for (int i = 0; i < normalVariants.size(); i++) {
            SaleVariant assigned = normalVariants.get(i);
            boolean isHidden = false;
            if (assigned.getVariantId() != null) {
                Product product = productMapper.selectById(assigned.getVariantId());
                if (product != null && product.getHiddenVariant() != null && product.getHiddenVariant()) {
                    isHidden = true;
                }
            }

            BlindBoxSlot slot = new BlindBoxSlot();
            slot.setSlotId(UUID.randomUUID().toString());
            slot.setMachineId(machineId);
            slot.setSetId(set.getSetId());
            slot.setSlotNo(i + 1);
            slot.setSlotCode("SLOT-" + (1000 + i + 1));
            slot.setSaleVariantId(assigned.getSaleVariantId());
            slot.setVariantId(assigned.getVariantId());
            slot.setIsHidden(isHidden);

            // 库存为0的款式直接显示已售出（占位）
            if (assigned.getStockQuantity() == null || assigned.getStockQuantity() <= 0) {
                slot.setStatus("SOLD");
                soldCount++;
            } else {
                slot.setStatus("AVAILABLE");
            }
            blindBoxSlotMapper.insert(slot);
        }

        // 更新套盒已售数
        if (soldCount > 0) {
            set.setSoldCount(soldCount);
            blindBoxSetMapper.updateById(set);
        }

        return set;
    }

    /**
     * 填充格位展示信息：
     * - AVAILABLE 但库存为0 → 临时标记为 SOLD（显示已售出，不写库）
     * - SOLD → 填充款式名称/图片
     * - AVAILABLE 且有库存 → 隐藏款式信息（防偷看）
     */
    private void populateSlotDisplayInfo(List<BlindBoxSlot> slots) {
        if (slots == null || slots.isEmpty()) return;

        // 批量查询所有 AVAILABLE 格位对应款式的库存
        Set<String> variantIdsToCheck = new HashSet<>();
        for (BlindBoxSlot slot : slots) {
            if (!"SOLD".equals(slot.getStatus()) && slot.getSaleVariantId() != null) {
                variantIdsToCheck.add(slot.getSaleVariantId());
            }
        }
        Map<String, Integer> stockMap = new HashMap<>();
        if (!variantIdsToCheck.isEmpty()) {
            List<SaleVariant> variants = saleVariantMapper.selectBatchIds(variantIdsToCheck);
            for (SaleVariant sv : variants) {
                stockMap.put(sv.getSaleVariantId(), sv.getStockQuantity() != null ? sv.getStockQuantity() : 0);
            }
        }

        for (BlindBoxSlot slot : slots) {
            if ("SOLD".equals(slot.getStatus())) {
                // 已售：填充款式信息用于展示
                fillSlotVariantInfo(slot);
            } else {
                // AVAILABLE：检查实时库存
                Integer stock = stockMap.get(slot.getSaleVariantId());
                if (stock == null || stock <= 0) {
                    // 库存为0，临时标记为已售出（显示款式图片让用户看到是哪个款售完）
                    slot.setStatus("SOLD");
                    fillSlotVariantInfo(slot);
                } else {
                    // 有库存：隐藏款式信息
                    slot.setSaleVariantId(null);
                    slot.setVariantId(null);
                    slot.setIsHidden(null);
                }
            }
        }
    }

    /**
     * 填充已售格位的款式信息
     */
    private void fillSlotVariantInfo(BlindBoxSlot slot) {
        if (slot.getSaleVariantId() != null) {
            SaleVariant variant = saleVariantMapper.selectById(slot.getSaleVariantId());
            if (variant != null) {
                slot.setVariantName(variant.getCustomDescription() != null
                        ? variant.getCustomDescription() : variant.getSkuCode());
                slot.setVariantImage(parseVariantImage(variant.getCustomImages()));
            }
        }
    }

    /**
     * 获取九宫格选盒状态
     * 如果该机器还没有槽位记录，则初始化9个槽位（每个预分配一个款式）
     */
    @Transactional(rollbackFor = Exception.class)
    public List<BlindBoxSlot> getMachineSlots(String machineId) {
        BlindBoxMachine machine = blindBoxMachineMapper.selectById(machineId);
        if (machine == null) {
            throw new RuntimeException("抽盒机不存在");
        }

        List<BlindBoxSlot> existingSlots = blindBoxSlotMapper.selectByMachineId(machineId);

        // 如果没有槽位或所有槽位都已售完，则重新生成9个槽位
        if (existingSlots.isEmpty() || blindBoxSlotMapper.countAvailableSlots(machineId) == 0) {
            // 如果有旧槽位全部已售完，先保留它们（历史记录），只生成新的
            if (!existingSlots.isEmpty() && blindBoxSlotMapper.countAvailableSlots(machineId) == 0) {
                // 所有都售完了，生成新一批9个槽位
            }
            initSlots(machineId, machine.getSaleSeriesId());
            existingSlots = blindBoxSlotMapper.selectByMachineId(machineId);
        }

        // 对未售出的槽位，隐藏款式信息（保持神秘感）
        for (BlindBoxSlot slot : existingSlots) {
            if (!"SOLD".equals(slot.getStatus())) {
                slot.setSaleVariantId(null);
                slot.setVariantId(null);
                slot.setIsHidden(null);
            } else {
                // 已售出的槽位，填充款式名称和图片
                if (slot.getSaleVariantId() != null) {
                    SaleVariant variant = saleVariantMapper.selectById(slot.getSaleVariantId());
                    if (variant != null) {
                        slot.setVariantName(variant.getCustomDescription() != null
                                ? variant.getCustomDescription() : variant.getSkuCode());
                        slot.setVariantImage(parseVariantImage(variant.getCustomImages()));
                    }
                }
            }
        }

        return existingSlots;
    }

    /**
     * 初始化9个槽位，每个预分配一个款式
     */
    private void initSlots(String machineId, String saleSeriesId) {
        // 获取有库存的款式
        LambdaQueryWrapper<SaleVariant> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SaleVariant::getSaleSeriesId, saleSeriesId)
               .eq(SaleVariant::getSaleStatus, "上架")
               .gt(SaleVariant::getStockQuantity, 0);
        List<SaleVariant> availableVariants = saleVariantMapper.selectList(wrapper);

        if (availableVariants.isEmpty()) {
            throw new RuntimeException("该系列已售罄，无法生成槽位");
        }

        // 为9个槽位各分配一个款式（按库存权重随机）
        List<SaleVariant> tempAvailable = new ArrayList<>(availableVariants);
        for (int i = 1; i <= 9; i++) {
            SaleVariant assigned = drawRandomVariant(tempAvailable);

            BlindBoxSlot slot = new BlindBoxSlot();
            slot.setSlotId(UUID.randomUUID().toString());
            slot.setMachineId(machineId);
            slot.setSlotNo(i);
            slot.setSlotCode("SLOT-" + (1000 + i));
            slot.setStatus("AVAILABLE");
            slot.setSaleVariantId(assigned.getSaleVariantId());
            slot.setVariantId(assigned.getVariantId());

            // 判断是否为隐藏款
            boolean isHidden = false;
            if (assigned.getVariantId() != null) {
                Product product = productMapper.selectById(assigned.getVariantId());
                if (product != null && product.getHiddenVariant() != null && product.getHiddenVariant()) {
                    isHidden = true;
                }
            }
            slot.setIsHidden(isHidden);

            blindBoxSlotMapper.insert(slot);

            // 减少本地库存计数，避免分配超过实际库存
            assigned.setStockQuantity(assigned.getStockQuantity() - 1);
            if (assigned.getStockQuantity() <= 0) {
                tempAvailable.remove(assigned);
            }
        }
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

        // 获取预分配的款式
        SaleVariant drawnVariant = saleVariantMapper.selectById(slot.getSaleVariantId());
        if (drawnVariant == null || drawnVariant.getStockQuantity() <= 0) {
            throw new RuntimeException("该款式库存不足");
        }

        // 判断是否为隐藏款
        boolean isHidden = false;
        if (drawnVariant.getVariantId() != null) {
            Product product = productMapper.selectById(drawnVariant.getVariantId());
            if (product != null && product.getHiddenVariant() != null && product.getHiddenVariant()) {
                isHidden = true;
            }
        }

        // 检查保底机制
        int userNonHiddenDraws = blindBoxDrawRecordMapper.countUserNonHiddenDraws(
                request.getMachineId(), request.getUserId());
        boolean isGuaranteed = machine.getGuaranteeDraws() > 0
                && userNonHiddenDraws >= machine.getGuaranteeDraws() - 1
                && !isHidden;

        // 扣减库存
        drawnVariant.setStockQuantity(drawnVariant.getStockQuantity() - 1);
        saleVariantMapper.updateById(drawnVariant);

        // 更新槽位状态为已售
        slot.setStatus("SOLD");
        slot.setDrawnBy(request.getUserId());
        slot.setDrawnAt(new Date());
        slot.setIsHidden(isHidden);
        blindBoxSlotMapper.updateById(slot);

        // 更新套盒已售数（如果有关联套盒）
        if (slot.getSetId() != null) {
            blindBoxSetMapper.incrementSoldCount(slot.getSetId());
        }

        // 更新抽盒机统计
        machine.setTotalDraws(machine.getTotalDraws() + 1);
        machine.setTotalStock(machine.getTotalStock() - 1);
        blindBoxMachineMapper.updateById(machine);

        // 记录抽盒历史
        BlindBoxDrawRecord record = new BlindBoxDrawRecord();
        record.setRecordId(UUID.randomUUID().toString());
        record.setMachineId(request.getMachineId());
        record.setUserId(request.getUserId());
        record.setSetId(slot.getSetId());
        record.setSlotNo(slot.getSlotNo());
        record.setSaleVariantId(drawnVariant.getSaleVariantId());
        record.setVariantId(drawnVariant.getVariantId());
        record.setDrawType("PICK");
        record.setIsHidden(isHidden);
        record.setIsGuaranteed(isGuaranteed);
        record.setDrawPrice(machine.getDrawPrice());
        record.setStatus("PENDING_OPEN");
        blindBoxDrawRecordMapper.insert(record);

        // 创建订单（作为支付凭证）
        String orderId = createPickOrder(machine, drawnVariant, request, machine.getDrawPrice(), isHidden);

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
                drawnVariant.getSaleVariantId(),
                isHidden,
                machine.getDrawPrice(),
                orderId
        );

        // 构建返回结果
        BlindBoxPickResultDTO result = new BlindBoxPickResultDTO();
        Order order = orderService.getOrderById(orderId);
        result.setOrderId(orderId);
        result.setOrderNo(order != null ? order.getOrderNo() : "");
        result.setSlotNo(slot.getSlotNo());
        result.setSlotCode(slot.getSlotCode());
        result.setSaleVariantId(drawnVariant.getSaleVariantId());
        result.setVariantId(drawnVariant.getVariantId());
        result.setVariantName(drawnVariant.getCustomDescription() != null
                ? drawnVariant.getCustomDescription() : drawnVariant.getSkuCode());
        result.setVariantImage(parseVariantImage(drawnVariant.getCustomImages()));
        result.setIsHidden(isHidden);
        result.setIsGuaranteed(isGuaranteed);
        result.setPrice(machine.getDrawPrice());
        result.setTotalPrice(machine.getDrawPrice());
        result.setStorageId(storage.getStorageId());

        return result;
    }

    /**
     * 创建选盒订单
     */
    private String createPickOrder(BlindBoxMachine machine, SaleVariant drawnVariant,
                                    BlindBoxPickRequestDTO request, BigDecimal price, boolean isHidden) {
        List<com.example.trendytoysocialecommercehd.dto.OrderItemRequest> orderItems = new ArrayList<>();

        com.example.trendytoysocialecommercehd.dto.OrderItemRequest orderItem = new com.example.trendytoysocialecommercehd.dto.OrderItemRequest();
        orderItem.setProductId(drawnVariant.getSaleVariantId());
        orderItem.setOriginalPrice(price);
        orderItem.setUnitPrice(price);
        orderItem.setQuantity(1);
        orderItem.setSubtotalAmount(price);
        orderItem.setAllocatedDiscount(BigDecimal.ZERO);
        orderItem.setActualSubtotal(price);
        orderItem.setItemSellerId(machine.getShopId());
        orderItem.setProductName(drawnVariant.getCustomDescription() != null
                ? drawnVariant.getCustomDescription() : drawnVariant.getSkuCode());
        orderItem.setProductImage(parseVariantImage(drawnVariant.getCustomImages()));
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

    /**
     * 解析款式图片
     */
    private String parseVariantImage(String customImages) {
        if (customImages == null || customImages.isEmpty()) {
            return "";
        }
        try {
            if (customImages.startsWith("[")) {
                String parsed = customImages.substring(1, customImages.length() - 1);
                if (parsed.contains(",")) {
                    return parsed.split(",")[0].trim().replaceAll("\"", "");
                }
                return parsed.trim().replaceAll("\"", "");
            }
            return customImages;
        } catch (Exception e) {
            return "";
        }
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
     * 商家端：保存款式覆盖配置（先删后插）
     * 仅保存 overrideStock=true 或 overrideProbability=true 的记录
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveMachineVariants(String machineId, List<BlindBoxMachineVariant> variants) {
        blindBoxMachineVariantMapper.deleteByMachineId(machineId);
        if (variants == null || variants.isEmpty()) {
            return;
        }
        for (BlindBoxMachineVariant v : variants) {
            // 仅当至少一个覆盖项为 true 时才入库
            boolean hasOverride = Boolean.TRUE.equals(v.getOverrideStock())
                    || Boolean.TRUE.equals(v.getOverrideProbability());
            if (!hasOverride) {
                continue;
            }
            v.setId(UUID.randomUUID().toString());
            v.setMachineId(machineId);
            if (v.getOverrideStock() == null) {
                v.setOverrideStock(false);
            }
            if (v.getOverrideProbability() == null) {
                v.setOverrideProbability(false);
            }
            blindBoxMachineVariantMapper.insert(v);
        }
        // 重新计算抽盒机总库存
        BlindBoxMachine machine = blindBoxMachineMapper.selectById(machineId);
        if (machine != null) {
            recalcMachineTotalStock(machine);
            blindBoxMachineMapper.updateById(machine);
        }
    }

    /**
     * 商家端：获取抽盒机款式覆盖配置（含 sale_variant 默认值）
     * 若该机器尚未配置过，则返回 sale_variant 的默认值（不写入数据库）
     */
    public List<BlindBoxMachineVariant> getMachineVariantsConfig(String machineId) {
        BlindBoxMachine machine = blindBoxMachineMapper.selectById(machineId);
        if (machine == null) {
            throw new RuntimeException("抽盒机不存在");
        }

        List<BlindBoxMachineVariant> existing =
                blindBoxMachineVariantMapper.selectByMachineIdWithInfo(machineId);

        // 若已有配置记录，直接返回
        if (!existing.isEmpty()) {
            return existing;
        }

        // 否则基于 sale_variant 生成默认配置（不写库）
        LambdaQueryWrapper<SaleVariant> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SaleVariant::getSaleSeriesId, machine.getSaleSeriesId())
               .eq(SaleVariant::getSaleStatus, "上架")
               .orderByAsc(SaleVariant::getCreatedAt);
        List<SaleVariant> saleVariants = saleVariantMapper.selectList(wrapper);

        List<BlindBoxMachineVariant> result = new ArrayList<>();
        for (SaleVariant sv : saleVariants) {
            BlindBoxMachineVariant v = new BlindBoxMachineVariant();
            v.setMachineId(machineId);
            v.setSaleVariantId(sv.getSaleVariantId());
            v.setVariantId(sv.getVariantId());
            v.setOverrideStock(false);
            v.setOverrideProbability(false);
            v.setOriginalStock(sv.getStockQuantity());
            v.setVariantName(sv.getCustomDescription());
            v.setVariantImage(parseVariantImage(sv.getCustomImages()));
            // 隐藏款标记
            if (sv.getVariantId() != null) {
                Product p = productMapper.selectById(sv.getVariantId());
                if (p != null && Boolean.TRUE.equals(p.getHiddenVariant())) {
                    v.setIsHidden(true);
                }
            }
            result.add(v);
        }
        return result;
    }

    /**
     * 商家端：更新抽盒机状态（启用/停用）
     * 状态约束：审核未通过的抽盒机不能启用
     */
    @Transactional(rollbackFor = Exception.class)
    public BlindBoxMachine updateMachineStatus(String machineId, String shopId, String status) {
        BlindBoxMachine machine = getMerchantMachine(machineId, shopId);
        if ("ACTIVE".equals(status) && !"APPROVED".equals(machine.getAuditStatus())) {
            throw new RuntimeException("审核未通过的抽盒机不能启用");
        }
        if ("TAKEDOWN".equals(status)) {
            throw new RuntimeException("商家无权设置强制下架状态");
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
     * 重新计算抽盒机总库存
     * 规则：
     *   - 覆盖库存的款式 -> 使用覆盖值
     *   - 未覆盖库存的款式 -> 使用 sale_variant.stock_quantity
     */
    private void recalcMachineTotalStock(BlindBoxMachine machine) {
        LambdaQueryWrapper<SaleVariant> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SaleVariant::getSaleSeriesId, machine.getSaleSeriesId())
               .eq(SaleVariant::getSaleStatus, "上架");
        List<SaleVariant> saleVariants = saleVariantMapper.selectList(wrapper);

        List<BlindBoxMachineVariant> overrides =
                blindBoxMachineVariantMapper.selectStockOverrides(machine.getMachineId());
        Map<String, Integer> overrideMap = new HashMap<>();
        for (BlindBoxMachineVariant ov : overrides) {
            if (ov.getStockQuantity() != null) {
                overrideMap.put(ov.getSaleVariantId(), ov.getStockQuantity());
            }
        }

        int total = 0;
        for (SaleVariant sv : saleVariants) {
            Integer stock = overrideMap.getOrDefault(sv.getSaleVariantId(), sv.getStockQuantity());
            if (stock != null) {
                total += stock;
            }
        }
        machine.setTotalStock(total);
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
            stat.setSaleVariantId((String) row.get("saleVariantId"));
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
