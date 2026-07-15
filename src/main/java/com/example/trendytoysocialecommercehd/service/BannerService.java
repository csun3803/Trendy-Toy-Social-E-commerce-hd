package com.example.trendytoysocialecommercehd.service;

import com.example.trendytoysocialecommercehd.entity.Banner;
import java.util.List;

public interface BannerService {
    List<Banner> getEnabledBanners();
    List<Banner> getAllBanners();
    Banner getBannerById(String bannerId);
    Banner createBanner(Banner banner);
    Banner updateBanner(Banner banner);
    void deleteBanner(String bannerId);
    void updateSortOrder(List<String> bannerIds);
    void toggleStatus(String bannerId);
}
