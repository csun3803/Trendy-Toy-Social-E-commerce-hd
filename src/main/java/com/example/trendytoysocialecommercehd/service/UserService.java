package com.example.trendytoysocialecommercehd.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.trendytoysocialecommercehd.dto.LoginDTO;
import com.example.trendytoysocialecommercehd.dto.RegisterDTO;
import com.example.trendytoysocialecommercehd.entity.User;

public interface UserService {
    User login(LoginDTO loginDTO);
    User register(RegisterDTO registerDTO);
    User getUserById(String userId);
    User updateAvatar(String userId, String avatarUrl);
    User getUserWithStats(String userId);
    void updateUserStats(String userId);

    // 管理员用户管理功能
    Page<User> getUserList(int page, int size, String accountStatus, String keyword);
    User updateUser(String userId, User user);
    void deleteUser(String userId);
    User updateUserStatus(String userId, String accountStatus);
}