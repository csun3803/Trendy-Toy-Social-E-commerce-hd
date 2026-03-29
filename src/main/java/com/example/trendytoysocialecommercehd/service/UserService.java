package com.example.trendytoysocialecommercehd.service;

import com.example.trendytoysocialecommercehd.dto.LoginDTO;
import com.example.trendytoysocialecommercehd.dto.RegisterDTO;
import com.example.trendytoysocialecommercehd.entity.User;

public interface UserService {
    User login(LoginDTO loginDTO);
    User register(RegisterDTO registerDTO);
    User getUserById(String userId);
    User updateAvatar(String userId, String avatarUrl);
}