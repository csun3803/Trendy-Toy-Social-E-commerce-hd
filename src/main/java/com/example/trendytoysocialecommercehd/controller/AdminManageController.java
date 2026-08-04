package com.example.trendytoysocialecommercehd.controller;

import com.example.trendytoysocialecommercehd.annotation.AuditLog;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.trendytoysocialecommercehd.common.Result;
import com.example.trendytoysocialecommercehd.dto.ShopWithStatsDTO;
import com.example.trendytoysocialecommercehd.entity.PlatformAdmin;
import com.example.trendytoysocialecommercehd.entity.Shop;
import com.example.trendytoysocialecommercehd.entity.ShopAdmin;
import com.example.trendytoysocialecommercehd.entity.User;
import com.example.trendytoysocialecommercehd.service.PlatformAdminService;
import com.example.trendytoysocialecommercehd.service.ShopAdminService;
import com.example.trendytoysocialecommercehd.service.ShopService;
import com.example.trendytoysocialecommercehd.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminManageController {

    @Autowired
    private ShopAdminService shopAdminService;

    @Autowired
    private PlatformAdminService platformAdminService;

    @Autowired
    private ShopService shopService;

    @Autowired
    private UserService userService;

    // ==================== 店铺管理员接口 ====================

    @GetMapping("/merchant/list")
    public Result<?> getMerchantAdminList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String auditStatus,
            @RequestParam(required = false) String isActive) {
        Page<ShopAdmin> result = shopAdminService.getShopAdminList(page, size, auditStatus, isActive);
        return Result.success(result);
    }

    @GetMapping("/merchant/{adminId}")
    public Result<?> getMerchantAdminById(@PathVariable String adminId) {
        ShopAdmin admin = shopAdminService.getById(adminId);
        if (admin == null) {
            return Result.error("管理员不存在");
        }
        admin.setPasswordHash(null);
        return Result.success(admin);
    }

    @AuditLog(module = "ADMIN", action = "CREATE", description = "创建店铺管理员")
    @PostMapping("/merchant")
    public Result<?> createMerchantAdmin(@RequestBody ShopAdmin admin) {
        try {
            shopAdminService.createShopAdmin(admin);
            return Result.success("创建成功");
        } catch (Exception e) {
            return Result.error("创建失败: " + e.getMessage());
        }
    }

    @AuditLog(module = "ADMIN", action = "UPDATE", description = "更新店铺管理员")
    @PutMapping("/merchant/{adminId}")
    public Result<?> updateMerchantAdmin(@PathVariable String adminId, @RequestBody ShopAdmin admin) {
        try {
            shopAdminService.updateShopAdmin(adminId, admin);
            return Result.success("更新成功");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @AuditLog(module = "ADMIN", action = "DELETE", description = "删除店铺管理员")
    @DeleteMapping("/merchant/{adminId}")
    public Result<?> deleteMerchantAdmin(@PathVariable String adminId) {
        shopAdminService.deleteShopAdmin(adminId);
        return Result.success("删除成功");
    }

    @AuditLog(module = "ADMIN", action = "UPDATE", description = "重置店铺管理员密码")
    @PutMapping("/merchant/{adminId}/reset-password")
    public Result<?> resetMerchantAdminPassword(@PathVariable String adminId) {
        try {
            shopAdminService.resetPassword(adminId);
            return Result.success("密码已重置为 123456");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @AuditLog(module = "ADMIN", action = "UPDATE", description = "切换店铺管理员状态")
    @PutMapping("/merchant/{adminId}/status")
    public Result<?> toggleMerchantAdminStatus(@PathVariable String adminId, @RequestBody ShopAdmin admin) {
        try {
            shopAdminService.toggleStatus(adminId, admin.getIsActive());
            return Result.success("状态更新成功");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    // ==================== 平台管理员接口 ====================

    @GetMapping("/platform/list")
    public Result<?> getPlatformAdminList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String adminLevel,
            @RequestParam(required = false) String accountStatus) {
        Page<PlatformAdmin> result = platformAdminService.getPlatformAdminList(page, size, adminLevel, accountStatus);
        return Result.success(result);
    }

    @GetMapping("/platform/{adminId}")
    public Result<?> getPlatformAdminById(@PathVariable String adminId) {
        PlatformAdmin admin = platformAdminService.getById(adminId);
        if (admin == null) {
            return Result.error("管理员不存在");
        }
        admin.setPasswordHash(null);
        return Result.success(admin);
    }

    @AuditLog(module = "ADMIN", action = "CREATE", description = "创建平台管理员")
    @PostMapping("/platform")
    public Result<?> createPlatformAdmin(@RequestBody PlatformAdmin admin) {
        try {
            platformAdminService.createPlatformAdmin(admin);
            return Result.success("创建成功");
        } catch (Exception e) {
            return Result.error("创建失败: " + e.getMessage());
        }
    }

    @AuditLog(module = "ADMIN", action = "UPDATE", description = "更新平台管理员")
    @PutMapping("/platform/{adminId}")
    public Result<?> updatePlatformAdmin(@PathVariable String adminId, @RequestBody PlatformAdmin admin) {
        try {
            platformAdminService.updatePlatformAdmin(adminId, admin);
            return Result.success("更新成功");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @AuditLog(module = "ADMIN", action = "DELETE", description = "删除平台管理员")
    @DeleteMapping("/platform/{adminId}")
    public Result<?> deletePlatformAdmin(@PathVariable String adminId) {
        platformAdminService.deletePlatformAdmin(adminId);
        return Result.success("删除成功");
    }

    // ==================== 商家管理接口 ====================

    @GetMapping("/shop/list")
    public Result<?> getShopList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String shopStatus,
            @RequestParam(required = false) String auditStatus) {
        Page<Shop> result = shopService.getShopList(page, size, shopStatus, auditStatus);
        return Result.success(result);
    }

    /**
     * 获取待审核的商家列表
     */
    @GetMapping("/shop/pending")
    public Result<?> getPendingShops(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<Shop> result = shopService.getPendingShops(page, size);
        return Result.success(result);
    }

    @AuditLog(module = "SHOP", action = "CREATE", description = "创建商家")
    @PostMapping("/shop")
    public Result<?> createShop(@RequestBody Shop shop) {
        try {
            shopService.createShop(shop);
            return Result.success("创建成功");
        } catch (Exception e) {
            return Result.error("创建失败: " + e.getMessage());
        }
    }

    @AuditLog(module = "SHOP", action = "UPDATE", description = "更新商家")
    @PutMapping("/shop/{shopId}")
    public Result<?> updateShop(@PathVariable String shopId, @RequestBody Shop shop) {
        try {
            shopService.updateShop(shopId, shop);
            return Result.success("更新成功");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @AuditLog(module = "SHOP", action = "DELETE", description = "删除商家")
    @DeleteMapping("/shop/{shopId}")
    public Result<?> deleteShop(@PathVariable String shopId) {
        shopService.deleteShop(shopId);
        return Result.success("删除成功");
    }

    @AuditLog(module = "SHOP", action = "APPROVE", description = "审核通过商家")
    @PutMapping("/shop/{shopId}/approve")
    public Result<?> approveShop(@PathVariable String shopId, @RequestBody Map<String, String> params) {
        try {
            String auditorId = params.get("auditorId");
            shopService.approveShop(shopId, auditorId);
            return Result.success("审核通过");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @AuditLog(module = "SHOP", action = "REJECT", description = "驳回商家申请")
    @PutMapping("/shop/{shopId}/reject")
    public Result<?> rejectShop(@PathVariable String shopId, @RequestBody Map<String, String> params) {
        try {
            String auditNotes = params.get("auditNotes");
            String auditorId = params.get("auditorId");
            shopService.rejectShop(shopId, auditNotes, auditorId);
            return Result.success("已驳回");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/shop/{shopId}")
    public Result<?> getShopById(@PathVariable String shopId) {
        Shop shop = shopService.getShopById(shopId);
        if (shop == null) {
            return Result.error("商家不存在");
        }
        return Result.success(shop);
    }

    /**
     * 开始审核商家（将状态从PENDING改为REVIEWING）
     */
    @AuditLog(module = "SHOP", action = "UPDATE", description = "开始审核商家")
    @PutMapping("/shop/{shopId}/start-review")
    public Result<?> startReview(@PathVariable String shopId) {
        try {
            shopService.startReview(shopId);
            return Result.success("已开始审核");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    // ==================== 用户管理接口 ====================

    @GetMapping("/user/list")
    public Result<?> getUserList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String accountStatus,
            @RequestParam(required = false) String keyword) {
        try {
            Page<User> userPage = userService.getUserList(page, size, accountStatus, keyword);
            return Result.success(userPage);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取用户列表失败");
        }
    }

    @GetMapping("/user/{userId}")
    public Result<?> getUserDetail(@PathVariable String userId) {
        try {
            User user = userService.getUserById(userId);
            if (user == null) {
                return Result.error("用户不存在");
            }
            return Result.success(user);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取用户详情失败");
        }
    }

    @AuditLog(module = "USER", action = "UPDATE", description = "更新用户信息")
    @PutMapping("/user/{userId}")
    public Result<?> updateUser(@PathVariable String userId, @RequestBody User user) {
        try {
            User updatedUser = userService.updateUser(userId, user);
            return Result.success(updatedUser);
        } catch (RuntimeException e) {
            e.printStackTrace();
            return Result.error(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("更新用户失败");
        }
    }

    @AuditLog(module = "USER", action = "DELETE", description = "删除用户")
    @DeleteMapping("/user/{userId}")
    public Result<?> deleteUser(@PathVariable String userId) {
        try {
            userService.deleteUser(userId);
            return Result.success("删除成功");
        } catch (RuntimeException e) {
            e.printStackTrace();
            return Result.error(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("删除用户失败");
        }
    }

    @AuditLog(module = "USER", action = "UPDATE", description = "更新用户状态")
    @PutMapping("/user/{userId}/status")
    public Result<?> updateUserStatus(
            @PathVariable String userId,
            @RequestBody Map<String, String> request) {
        try {
            String accountStatus = request.get("accountStatus");
            User user = userService.updateUserStatus(userId, accountStatus);
            return Result.success(user);
        } catch (RuntimeException e) {
            e.printStackTrace();
            return Result.error(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("更新用户状态失败");
        }
    }
}
