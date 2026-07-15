package com.example.trendytoysocialecommercehd.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.trendytoysocialecommercehd.entity.UserProductFavorite;
import com.example.trendytoysocialecommercehd.mapper.UserProductFavoriteMapper;
import com.example.trendytoysocialecommercehd.service.UserProductFavoriteService;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserProductFavoriteServiceImpl extends ServiceImpl<UserProductFavoriteMapper, UserProductFavorite> implements UserProductFavoriteService {

    @Override
    public boolean isFavorite(String userId, String productId) {
        LambdaQueryWrapper<UserProductFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserProductFavorite::getUserId, userId)
                .eq(UserProductFavorite::getProductId, productId)
                .eq(UserProductFavorite::getStatus, "active");
        return count(wrapper) > 0;
    }

    @Override
    public boolean toggleFavorite(String userId, String productId) {
        if (isFavorite(userId, productId)) {
            return removeFavorite(userId, productId);
        } else {
            return addFavorite(userId, productId);
        }
    }

    @Override
    public boolean addFavorite(String userId, String productId) {
        LambdaQueryWrapper<UserProductFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserProductFavorite::getUserId, userId)
                .eq(UserProductFavorite::getProductId, productId);

        UserProductFavorite existing = getOne(wrapper);
        if (existing != null) {
            existing.setStatus("active");
            return updateById(existing);
        }

        UserProductFavorite favorite = new UserProductFavorite();
        favorite.setFavoriteId(UUID.randomUUID().toString());
        favorite.setUserId(userId);
        favorite.setProductId(productId);
        favorite.setStatus("active");
        return save(favorite);
    }

    @Override
    public boolean removeFavorite(String userId, String productId) {
        LambdaQueryWrapper<UserProductFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserProductFavorite::getUserId, userId)
                .eq(UserProductFavorite::getProductId, productId);
        UserProductFavorite favorite = getOne(wrapper);
        if (favorite != null) {
            favorite.setStatus("inactive");
            return updateById(favorite);
        }
        return false;
    }
}