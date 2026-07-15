package com.example.trendytoysocialecommercehd.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.trendytoysocialecommercehd.entity.Banner;
import com.example.trendytoysocialecommercehd.mapper.BannerMapper;
import com.example.trendytoysocialecommercehd.service.BannerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class BannerServiceImpl implements BannerService {

    @Autowired
    private BannerMapper bannerMapper;

    @Override
    public List<Banner> getEnabledBanners() {
        QueryWrapper<Banner> wrapper = new QueryWrapper<>();
        wrapper.eq("status", "ENABLED")
                .orderByAsc("sort_order");
        return bannerMapper.selectList(wrapper);
    }

    @Override
    public List<Banner> getAllBanners() {
        QueryWrapper<Banner> wrapper = new QueryWrapper<>();
        wrapper.orderByAsc("sort_order");
        return bannerMapper.selectList(wrapper);
    }

    @Override
    public Banner getBannerById(String bannerId) {
        QueryWrapper<Banner> wrapper = new QueryWrapper<>();
        wrapper.eq("banner_id", bannerId);
        return bannerMapper.selectOne(wrapper);
    }

    @Override
    public Banner createBanner(Banner banner) {
        banner.setBannerId(UUID.randomUUID().toString());
        banner.setSortOrder(banner.getSortOrder() != null ? banner.getSortOrder() : 0);
        banner.setJumpType(banner.getJumpType() != null ? banner.getJumpType() : "NONE");
        banner.setJumpValue(banner.getJumpValue() != null ? banner.getJumpValue() : "");
        banner.setStatus(banner.getStatus() != null ? banner.getStatus() : "ENABLED");
        banner.setCreatedAt(new Date());
        banner.setUpdatedAt(new Date());
        bannerMapper.insert(banner);
        return banner;
    }

    @Override
    public Banner updateBanner(Banner banner) {
        Banner existing = getBannerById(banner.getBannerId());
        if (existing == null) {
            throw new RuntimeException("轮播图不存在");
        }
        banner.setId(existing.getId());
        banner.setUpdatedAt(new Date());
        bannerMapper.updateById(banner);
        return banner;
    }

    @Override
    public void deleteBanner(String bannerId) {
        QueryWrapper<Banner> wrapper = new QueryWrapper<>();
        wrapper.eq("banner_id", bannerId);
        bannerMapper.delete(wrapper);
    }

    @Override
    public void updateSortOrder(List<String> bannerIds) {
        for (int i = 0; i < bannerIds.size(); i++) {
            String bannerId = bannerIds.get(i);
            Banner banner = getBannerById(bannerId);
            if (banner != null) {
                banner.setSortOrder(i);
                banner.setUpdatedAt(new Date());
                bannerMapper.updateById(banner);
            }
        }
    }

    @Override
    public void toggleStatus(String bannerId) {
        Banner banner = getBannerById(bannerId);
        if (banner == null) {
            throw new RuntimeException("轮播图不存在");
        }
        String newStatus = "ENABLED".equals(banner.getStatus()) ? "DISABLED" : "ENABLED";
        banner.setStatus(newStatus);
        banner.setUpdatedAt(new Date());
        bannerMapper.updateById(banner);
    }
}
