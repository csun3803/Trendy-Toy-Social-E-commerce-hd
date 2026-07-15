package com.example.trendytoysocialecommercehd.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.trendytoysocialecommercehd.entity.UserProductFavorite;

public interface UserProductFavoriteService extends IService<UserProductFavorite> {
    boolean isFavorite(String userId, String productId);
    boolean toggleFavorite(String userId, String productId);
    boolean addFavorite(String userId, String productId);
    boolean removeFavorite(String userId, String productId);
}