package com.example.trendytoysocialecommercehd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.trendytoysocialecommercehd.dto.ShopApplyDTO;
import com.example.trendytoysocialecommercehd.dto.ShopUpdateDTO;
import com.example.trendytoysocialecommercehd.entity.Shop;
import com.example.trendytoysocialecommercehd.entity.ShopConfig;
import com.example.trendytoysocialecommercehd.entity.ShopFinance;
import com.example.trendytoysocialecommercehd.entity.PlatformAdmin;
import com.example.trendytoysocialecommercehd.mapper.ShopMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class ShopService extends ServiceImpl<ShopMapper, Shop> {

    @Autowired
    private ShopMapper shopMapper;

    @Autowired
    private ShopFinanceService shopFinanceService;

    @Autowired
    private ShopConfigService shopConfigService;

    @Autowired
    private ShopAdminService shopAdminService;

    @Autowired
    private ShopCertificationFileService shopCertificationFileService;

    @Autowired
    private PlatformAdminService platformAdminService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Page<Shop> getShopList(int page, int size, String shopStatus, String auditStatus) {
        Page<Shop> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<Shop> wrapper = new LambdaQueryWrapper<>();

        if (shopStatus != null && !shopStatus.isEmpty()) {
            wrapper.eq(Shop::getShopStatus, shopStatus);
        }
        if (auditStatus != null && !auditStatus.isEmpty()) {
            wrapper.eq(Shop::getAuditStatus, auditStatus);
        } else {
            // 未指定审核状态时，默认排除草稿（草稿是商家未提交的内部状态）
            wrapper.ne(Shop::getAuditStatus, "DRAFT");
        }
        wrapper.orderByDesc(Shop::getShopId);

        Page<Shop> result = this.page(pageObj, wrapper);
        // 为每个店铺添加销量数据
        for (Shop shop : result.getRecords()) {
            addSalesDataToShop(shop);
            populateFileFields(shop);
        }
        return result;
    }

    public Shop getShopById(String shopId) {
        Shop shop = this.getById(shopId);
        if (shop != null) {
            addSalesDataToShop(shop);
            populateFileFields(shop);
        }
        return shop;
    }

    // 直接给Shop对象添加销量数据
    private void addSalesDataToShop(Shop shop) {
        Integer monthlySales = shopMapper.getMonthlySales(shop.getShopId());
        Integer totalSales = shopMapper.getTotalSales(shop.getShopId());
        BigDecimal totalSalesAmount = shopMapper.getTotalSalesAmount(shop.getShopId());

        shop.setMonthlySales(monthlySales);
        shop.setTotalSales(totalSales);
        shop.setTotalSalesAmount(totalSalesAmount);
    }

    // 从 shop_certification_file 表填充文件 URL 到 Shop 的 transient 字段，并填充财务信息
    private void populateFileFields(Shop shop) {
        if (shop == null || shop.getShopId() == null) {
            return;
        }
        Map<String, String> fileUrls = shopCertificationFileService.getFileUrlsByShopId(shop.getShopId());
        shop.setLicenseImage(fileUrls.get("business_license"));
        shop.setIdCardFront(fileUrls.get("id_card_front"));
        shop.setIdCardBack(fileUrls.get("id_card_back"));

        // 填充财务信息
        ShopFinance finance = shopFinanceService.getByShopId(shop.getShopId());
        if (finance != null) {
            shop.setBankName(finance.getBankName());
            shop.setBankAccount(finance.getBankAccount());
            shop.setAccountHolder(finance.getAccountHolder());
            shop.setBranchName(finance.getBranchName());
        }

        // 填充审核员名称
        if (shop.getAuditorId() != null && !shop.getAuditorId().isEmpty()) {
            try {
                PlatformAdmin auditor = platformAdminService.getById(shop.getAuditorId());
                if (auditor != null) {
                    shop.setAuditorName(auditor.getEmployeeId() != null && !auditor.getEmployeeId().isEmpty()
                        ? auditor.getEmployeeId() : auditor.getAdminId());
                }
            } catch (Exception e) {
                // 审核员可能不存在，忽略异常
            }
        }
    }

    public void createShop(Shop shop) {
        this.save(shop);
    }

    public void updateShop(String shopId, Shop shop) {
        Shop existing = this.getById(shopId);
        if (existing == null) {
            throw new RuntimeException("商家不存在");
        }
        shop.setShopId(shopId);
        this.updateById(shop);
    }

    public void deleteShop(String shopId) {
        this.removeById(shopId);
    }

    /**
     * 商家提交入驻申请
     */
    @Transactional
    public Shop applyShop(ShopApplyDTO dto, String adminId) {
        // 生成shopId和applySn
        String shopId = "shop_" + System.currentTimeMillis();
        String applySn = generateApplySn();

        Shop shop = new Shop();
        shop.setShopId(shopId);
        shop.setApplySn(applySn);

        // 资质类
        shop.setSubjectType(dto.getSubjectType());
        shop.setLegalPersonIdCardNumber(dto.getLegalPersonIdCardNumber());
        shop.setLegalPersonIdCardExpiry(dto.getLegalPersonIdCardExpiry());
        shop.setUnifiedSocialCreditCode(dto.getUnifiedSocialCreditCode());
        shop.setLegalPersonName(dto.getLegalPersonName());

        // 品牌信息
        shop.setHasBrand(dto.getHasBrand());
        shop.setBrandAuthorizationLetter(dto.getBrandAuthorizationLetter());
        shop.setTrademarkRegistrationCert(dto.getTrademarkRegistrationCert());

        // 身份类 (文件URL保存到 shop_certification_file 表)
        shop.setShopName(dto.getShopName());
        shop.setBusinessLicenseExpiry(dto.getBusinessLicenseExpiry());

        // 联系类
        shop.setContactPerson(dto.getContactPerson());
        shop.setContactPhone(dto.getContactPhone());
        shop.setContactEmail(dto.getContactEmail());
        shop.setMainCategories(dto.getMainCategories());

        // 配置类
        shop.setShopCover(dto.getShopCover());
        shop.setShopIntro(dto.getShopIntro());
        shop.setCustomerServicePhone(dto.getCustomerServicePhone());
        shop.setCustomerServiceEmail(dto.getCustomerServiceEmail());
        shop.setReturnAddressProvince(dto.getReturnAddressProvince());
        shop.setReturnAddressCity(dto.getReturnAddressCity());
        shop.setReturnAddressDistrict(dto.getReturnAddressDistrict());
        shop.setReturnAddressDetail(dto.getReturnAddressDetail());
        shop.setReturnAddressContact(dto.getReturnAddressContact());
        shop.setReturnAddressPhone(dto.getReturnAddressPhone());

        // 状态（统一英文表示）
        shop.setAuditStatus("PENDING");
        shop.setShopStatus("PENDING_OPERATIONS");
        shop.setAuditRound(1);
        shop.setShopRating(BigDecimal.valueOf(5.0));

        this.save(shop);

        // 保存文件 URL 到 shop_certification_file 表
        // 优先使用 dto.files（带详细信息），否则回退到旧的方式（仅 URL）
        if (dto.getFiles() != null && !dto.getFiles().isEmpty()) {
            shopCertificationFileService.saveFileDetails(shop.getShopId(), dto.getFiles());
        } else {
            Map<String, String> fileMap = new HashMap<>();
            if (dto.getLicenseImage() != null) fileMap.put("business_license", dto.getLicenseImage());
            if (dto.getIdCardFront() != null) fileMap.put("id_card_front", dto.getIdCardFront());
            if (dto.getIdCardBack() != null) fileMap.put("id_card_back", dto.getIdCardBack());
            shopCertificationFileService.saveFilesFromMap(shop.getShopId(), fileMap);
        }

        // 创建 ShopFinance 记录
        ShopFinance finance = new ShopFinance();
        finance.setShopId(shopId);
        finance.setBankName(dto.getBankName());
        finance.setBankAccount(dto.getBankAccount());
        finance.setAccountHolder(dto.getAccountHolder());
        finance.setBranchName(dto.getBranchName());
        finance.setDepositStatus("未缴纳");
        shopFinanceService.save(finance);

        // 创建 ShopConfig 默认配置
        ShopConfig config = new ShopConfig();
        config.setShopId(shopId);
        config.setAuthenticityGuarantee(1);
        config.setPlatformCommissionRate(BigDecimal.ZERO);
        config.setTechServiceRate(BigDecimal.ZERO);
        shopConfigService.save(config);

        // 关联 ShopAdmin
        shopAdminService.updateShopId(adminId, shopId);

        // 填充文件 URL 到 transient 字段 (便于前端直接使用返回的 shop 对象)
        populateFileFields(shop);

        return shop;
    }

    /**
     * 商家被拒绝后重新提交申请（覆盖原有数据，重置审核状态）
     */
    public Shop resubmitShop(String shopId, ShopApplyDTO dto) {
        Shop shop = this.getById(shopId);
        if (shop == null) {
            throw new RuntimeException("店铺不存在");
        }
        if (!"REJECTED".equals(shop.getAuditStatus()) && !"DRAFT".equals(shop.getAuditStatus())) {
            throw new RuntimeException("当前状态不允许重新提交");
        }

        // 覆盖所有字段（与 applyShop 一致）
        applyDtoToShop(shop, dto);

        // 重置审核状态，增加审核轮次
        shop.setAuditStatus("PENDING");
        shop.setAuditNotes(null);
        shop.setAuditedAt(null);
        shop.setAuditorId(null);
        shop.setAuditRound((shop.getAuditRound() == null ? 0 : shop.getAuditRound()) + 1);

        this.updateById(shop);

        // 更新资质文件（优先使用带详细信息的方式）
        if (dto.getFiles() != null && !dto.getFiles().isEmpty()) {
            shopCertificationFileService.saveFileDetails(shopId, dto.getFiles());
        } else if (dto.getLicenseImage() != null || dto.getIdCardFront() != null || dto.getIdCardBack() != null) {
            Map<String, String> fileMap = new HashMap<>();
            if (dto.getLicenseImage() != null) fileMap.put("business_license", dto.getLicenseImage());
            if (dto.getIdCardFront() != null) fileMap.put("id_card_front", dto.getIdCardFront());
            if (dto.getIdCardBack() != null) fileMap.put("id_card_back", dto.getIdCardBack());
            shopCertificationFileService.saveFilesFromMap(shopId, fileMap);
        }

        // 更新财务信息
        ShopFinance existingFinance = shopFinanceService.getByShopId(shopId);
        if (existingFinance == null) {
            ShopFinance finance = new ShopFinance();
            finance.setShopId(shopId);
            finance.setBankName(dto.getBankName());
            finance.setBankAccount(dto.getBankAccount());
            finance.setAccountHolder(dto.getAccountHolder());
            finance.setBranchName(dto.getBranchName());
            finance.setDepositStatus("未缴纳");
            shopFinanceService.save(finance);
        } else {
            if (dto.getBankName() != null) existingFinance.setBankName(dto.getBankName());
            if (dto.getBankAccount() != null) existingFinance.setBankAccount(dto.getBankAccount());
            if (dto.getAccountHolder() != null) existingFinance.setAccountHolder(dto.getAccountHolder());
            if (dto.getBranchName() != null) existingFinance.setBranchName(dto.getBranchName());
            shopFinanceService.updateById(existingFinance);
        }

        populateFileFields(shop);
        return shop;
    }

    // 将 DTO 字段映射到 Shop 实体（applyShop 和 resubmitShop 共用）
    private void applyDtoToShop(Shop shop, ShopApplyDTO dto) {
        shop.setSubjectType(dto.getSubjectType());
        shop.setLegalPersonName(dto.getLegalPersonName());
        shop.setLegalPersonIdCardNumber(dto.getLegalPersonIdCardNumber());
        shop.setLegalPersonIdCardExpiry(dto.getLegalPersonIdCardExpiry());
        shop.setUnifiedSocialCreditCode(dto.getUnifiedSocialCreditCode());
        shop.setBusinessLicenseExpiry(dto.getBusinessLicenseExpiry());
        shop.setHasBrand(dto.getHasBrand());
        shop.setBrandAuthorizationLetter(dto.getBrandAuthorizationLetter());
        shop.setTrademarkRegistrationCert(dto.getTrademarkRegistrationCert());
        shop.setShopName(dto.getShopName());
        shop.setShopCover(dto.getShopCover());
        shop.setShopIntro(dto.getShopIntro());
        shop.setContactPerson(dto.getContactPerson());
        shop.setContactPhone(dto.getContactPhone());
        shop.setContactEmail(dto.getContactEmail());
        shop.setMainCategories(dto.getMainCategories());
        shop.setCustomerServicePhone(dto.getCustomerServicePhone());
        shop.setCustomerServiceEmail(dto.getCustomerServiceEmail());
        shop.setReturnAddressProvince(dto.getReturnAddressProvince());
        shop.setReturnAddressCity(dto.getReturnAddressCity());
        shop.setReturnAddressDistrict(dto.getReturnAddressDistrict());
        shop.setReturnAddressDetail(dto.getReturnAddressDetail());
        shop.setReturnAddressContact(dto.getReturnAddressContact());
        shop.setReturnAddressPhone(dto.getReturnAddressPhone());
    }

    /**
     * 商家撤回入驻申请（仅 PENDING 状态可撤回）
     * 撤回后审核状态变为 DRAFT，商家可继续编辑并重新提交
     */
    public Shop withdrawShop(String shopId) {
        Shop shop = this.getById(shopId);
        if (shop == null) {
            throw new RuntimeException("店铺不存在");
        }
        if (!"PENDING".equals(shop.getAuditStatus())) {
            throw new RuntimeException("当前状态不允许撤回");
        }
        shop.setAuditStatus("DRAFT");
        shop.setAuditedAt(null);
        shop.setAuditorId(null);
        this.updateById(shop);
        populateFileFields(shop);
        return shop;
    }

    /**
     * 按分类更新店铺信息
     */
    @Transactional
    public Shop updateShopByCategory(String shopId, ShopUpdateDTO dto) {
        Shop shop = this.getById(shopId);
        if (shop == null) {
            throw new RuntimeException("商家不存在");
        }

        String category = dto.getCategory();
        if (category == null || category.isEmpty()) {
            throw new RuntimeException("请指定更新分类");
        }

        switch (category) {
            case "qualification":
                // 资质类：仅在审核通过前可修改
                if ("APPROVED".equals(shop.getAuditStatus())) {
                    throw new RuntimeException("审核通过后资质信息不可修改");
                }
                if (dto.getSubjectType() != null) shop.setSubjectType(dto.getSubjectType());
                if (dto.getLegalPersonIdCardNumber() != null) shop.setLegalPersonIdCardNumber(dto.getLegalPersonIdCardNumber());
                if (dto.getLegalPersonIdCardExpiry() != null) shop.setLegalPersonIdCardExpiry(dto.getLegalPersonIdCardExpiry());
                if (dto.getUnifiedSocialCreditCode() != null) shop.setUnifiedSocialCreditCode(dto.getUnifiedSocialCreditCode());
                if (dto.getLegalPersonName() != null) shop.setLegalPersonName(dto.getLegalPersonName());
                if (dto.getHasBrand() != null) shop.setHasBrand(dto.getHasBrand());
                if (dto.getBrandAuthorizationLetter() != null) shop.setBrandAuthorizationLetter(dto.getBrandAuthorizationLetter());
                if (dto.getTrademarkRegistrationCert() != null) shop.setTrademarkRegistrationCert(dto.getTrademarkRegistrationCert());
                break;

            case "identity":
                // 身份类：变更存入 pendingData，等待管理员审核
                Map<String, Object> pendingMap = new HashMap<>();
                if (dto.getShopName() != null) pendingMap.put("shopName", dto.getShopName());
                if (dto.getLicenseImage() != null) pendingMap.put("licenseImage", dto.getLicenseImage());
                if (dto.getIdCardFront() != null) pendingMap.put("idCardFront", dto.getIdCardFront());
                if (dto.getIdCardBack() != null) pendingMap.put("idCardBack", dto.getIdCardBack());
                if (dto.getBusinessLicenseExpiry() != null) pendingMap.put("businessLicenseExpiry", dto.getBusinessLicenseExpiry());

                if (!pendingMap.isEmpty()) {
                    try {
                        shop.setPendingData(objectMapper.writeValueAsString(pendingMap));
                    } catch (Exception e) {
                        throw new RuntimeException("序列化待审核数据失败");
                    }
                    shop.setAuditStatus("PENDING");
                    shop.setAuditRound(shop.getAuditRound() + 1);
                }
                break;

            case "contact":
                // 联系类：直接更新，无需审核
                if (dto.getContactPerson() != null) shop.setContactPerson(dto.getContactPerson());
                if (dto.getContactPhone() != null) shop.setContactPhone(dto.getContactPhone());
                if (dto.getContactEmail() != null) shop.setContactEmail(dto.getContactEmail());
                if (dto.getMainCategories() != null) shop.setMainCategories(dto.getMainCategories());
                break;

            case "config":
                // 配置类：直接更新，无需审核
                if (dto.getShopCover() != null) shop.setShopCover(dto.getShopCover());
                if (dto.getShopIntro() != null) shop.setShopIntro(dto.getShopIntro());
                if (dto.getCustomerServicePhone() != null) shop.setCustomerServicePhone(dto.getCustomerServicePhone());
                if (dto.getCustomerServiceEmail() != null) shop.setCustomerServiceEmail(dto.getCustomerServiceEmail());
                if (dto.getReturnAddressProvince() != null) shop.setReturnAddressProvince(dto.getReturnAddressProvince());
                if (dto.getReturnAddressCity() != null) shop.setReturnAddressCity(dto.getReturnAddressCity());
                if (dto.getReturnAddressDistrict() != null) shop.setReturnAddressDistrict(dto.getReturnAddressDistrict());
                if (dto.getReturnAddressDetail() != null) shop.setReturnAddressDetail(dto.getReturnAddressDetail());
                if (dto.getReturnAddressContact() != null) shop.setReturnAddressContact(dto.getReturnAddressContact());
                if (dto.getReturnAddressPhone() != null) shop.setReturnAddressPhone(dto.getReturnAddressPhone());
                break;

            case "finance":
                // 财务类：直接更新到 shop_finance 表
                ShopFinance finance = shopFinanceService.getByShopId(shopId);
                if (finance == null) {
                    finance = new ShopFinance();
                    finance.setShopId(shopId);
                    finance.setDepositStatus("未缴纳");
                }
                if (dto.getBankName() != null) finance.setBankName(dto.getBankName());
                if (dto.getBankAccount() != null) finance.setBankAccount(dto.getBankAccount());
                if (dto.getAccountHolder() != null) finance.setAccountHolder(dto.getAccountHolder());
                if (dto.getBranchName() != null) finance.setBranchName(dto.getBranchName());
                shopFinanceService.saveOrUpdateByShopId(shopId, finance);
                // 财务更新不需要更新shop表，直接返回
                this.updateById(shop);
                populateFileFields(shop);
                return shop;

            default:
                throw new RuntimeException("无效的更新分类: " + category);
        }

        this.updateById(shop);
        populateFileFields(shop);
        return shop;
    }

    /**
     * 审核通过
     */
    @Transactional
    public void approveShop(String shopId, String auditorId) {
        Shop shop = this.getById(shopId);
        if (shop == null) {
            throw new RuntimeException("商家不存在");
        }
        shop.setAuditStatus("APPROVED");
        shop.setShopStatus("ACTIVE");
        shop.setBusinessStatus("OPERATING");
        if (auditorId != null) {
            shop.setAuditorId(auditorId);
        }
        shop.setAuditedAt(LocalDateTime.now());

        // 如果有待审核的身份变更，应用变更并清空pendingData
        if (shop.getPendingData() != null && !shop.getPendingData().isEmpty()) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> pendingMap = objectMapper.readValue(shop.getPendingData(), Map.class);
                if (pendingMap.containsKey("shopName")) shop.setShopName((String) pendingMap.get("shopName"));
                if (pendingMap.containsKey("businessLicenseExpiry")) shop.setBusinessLicenseExpiry((String) pendingMap.get("businessLicenseExpiry"));
                // 将待审核的文件 URL 保存到 shop_certification_file 表
                if (pendingMap.containsKey("licenseImage")) {
                    shopCertificationFileService.saveOrUpdateFile(shopId, "business_license", (String) pendingMap.get("licenseImage"));
                }
                if (pendingMap.containsKey("idCardFront")) {
                    shopCertificationFileService.saveOrUpdateFile(shopId, "id_card_front", (String) pendingMap.get("idCardFront"));
                }
                if (pendingMap.containsKey("idCardBack")) {
                    shopCertificationFileService.saveOrUpdateFile(shopId, "id_card_back", (String) pendingMap.get("idCardBack"));
                }
                shop.setPendingData(null);
            } catch (Exception e) {
                throw new RuntimeException("解析待审核数据失败");
            }
        }

        this.updateById(shop);

        // 更新 ShopAdmin 审核状态
        if (shop.getShopId() != null) {
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.example.trendytoysocialecommercehd.entity.ShopAdmin> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
            wrapper.eq(com.example.trendytoysocialecommercehd.entity.ShopAdmin::getShopId, shopId);
            com.example.trendytoysocialecommercehd.entity.ShopAdmin shopAdmin = shopAdminService.getOne(wrapper);
            if (shopAdmin != null) {
                shopAdmin.setAuditStatus("APPROVED");
                shopAdminService.updateById(shopAdmin);
            }
        }
    }

    /**
     * 审核驳回
     */
    @Transactional
    public void rejectShop(String shopId, String auditNotes, String auditorId) {
        Shop shop = this.getById(shopId);
        if (shop == null) {
            throw new RuntimeException("商家不存在");
        }
        shop.setAuditStatus("REJECTED");
        shop.setAuditNotes(auditNotes);
        if (auditorId != null) {
            shop.setAuditorId(auditorId);
        }
        shop.setAuditedAt(LocalDateTime.now());

        // 如果有pendingData，清空（拒绝待审核的变更）
        if (shop.getPendingData() != null && !shop.getPendingData().isEmpty()) {
            shop.setPendingData(null);
        }

        this.updateById(shop);

        // 更新 ShopAdmin 审核状态
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.example.trendytoysocialecommercehd.entity.ShopAdmin> wrapper =
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        wrapper.eq(com.example.trendytoysocialecommercehd.entity.ShopAdmin::getShopId, shopId);
        com.example.trendytoysocialecommercehd.entity.ShopAdmin shopAdmin = shopAdminService.getOne(wrapper);
        if (shopAdmin != null) {
            shopAdmin.setAuditStatus("REJECTED");
            shopAdmin.setAuditNotes(auditNotes);
            shopAdminService.updateById(shopAdmin);
        }
    }

    /**
     * 开始审核（将状态从PENDING改为REVIEWING）
     */
    public void startReview(String shopId) {
        Shop shop = this.getById(shopId);
        if (shop == null) {
            throw new RuntimeException("商家不存在");
        }
        if (!"PENDING".equals(shop.getAuditStatus())) {
            throw new RuntimeException("当前状态不允许开始审核，仅待审核状态可操作");
        }
        shop.setAuditStatus("REVIEWING");
        this.updateById(shop);
    }

    /**
     * 获取待审核的商家列表（包含PENDING和REVIEWING状态）
     */
    public Page<Shop> getPendingShops(int page, int size) {
        Page<Shop> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<Shop> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Shop::getAuditStatus, "PENDING", "REVIEWING");
        wrapper.orderByDesc(Shop::getCreatedAt);
        Page<Shop> result = this.page(pageObj, wrapper);
        for (Shop shop : result.getRecords()) {
            populateFileFields(shop);
        }
        return result;
    }

    /**
     * 生成申请单号
     */
    private String generateApplySn() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int random = ThreadLocalRandom.current().nextInt(1000, 9999);
        return "MA" + timestamp + random;
    }
}
