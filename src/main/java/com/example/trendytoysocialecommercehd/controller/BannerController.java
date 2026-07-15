package com.example.trendytoysocialecommercehd.controller;

import com.example.trendytoysocialecommercehd.common.Result;
import com.example.trendytoysocialecommercehd.entity.Banner;
import com.example.trendytoysocialecommercehd.service.BannerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/banner")
public class BannerController {

    @Autowired
    private BannerService bannerService;

    // 用户端：获取启用的轮播图列表
    @GetMapping("/list")
    public Result<?> getEnabledBanners() {
        return Result.success(bannerService.getEnabledBanners());
    }

    // 管理端：获取所有轮播图
    @GetMapping("/admin/list")
    public Result<?> getAllBanners() {
        return Result.success(bannerService.getAllBanners());
    }

    // 管理端：获取单个轮播图
    @GetMapping("/admin/{bannerId}")
    public Result<?> getBannerById(@PathVariable String bannerId) {
        Banner banner = bannerService.getBannerById(bannerId);
        if (banner == null) {
            return Result.error("轮播图不存在");
        }
        return Result.success(banner);
    }

    // 管理端：新增轮播图
    @PostMapping("/admin")
    public Result<?> createBanner(@RequestBody Banner banner) {
        Banner result = bannerService.createBanner(banner);
        return Result.success(result);
    }

    // 管理端：编辑轮播图
    @PutMapping("/admin")
    public Result<?> updateBanner(@RequestBody Banner banner) {
        try {
            Banner result = bannerService.updateBanner(banner);
            return Result.success(result);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    // 管理端：删除轮播图
    @DeleteMapping("/admin/{bannerId}")
    public Result<?> deleteBanner(@PathVariable String bannerId) {
        bannerService.deleteBanner(bannerId);
        return Result.success("删除成功");
    }

    // 管理端：更新排序
    @PutMapping("/admin/sort")
    public Result<?> updateSortOrder(@RequestBody Map<String, List<String>> body) {
        List<String> bannerIds = body.get("bannerIds");
        if (bannerIds == null) {
            return Result.error("缺少 bannerIds 参数");
        }
        bannerService.updateSortOrder(bannerIds);
        return Result.success("排序更新成功");
    }

    // 管理端：切换上下架状态
    @PutMapping("/admin/toggle/{bannerId}")
    public Result<?> toggleStatus(@PathVariable String bannerId) {
        try {
            bannerService.toggleStatus(bannerId);
            return Result.success("状态切换成功");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }
}
