package com.example.trendytoysocialecommercehd.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.trendytoysocialecommercehd.common.Result;
import com.example.trendytoysocialecommercehd.dto.LoginDTO;
import com.example.trendytoysocialecommercehd.dto.RegisterDTO;
import com.example.trendytoysocialecommercehd.entity.User;
import com.example.trendytoysocialecommercehd.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.example.trendytoysocialecommercehd.util.JwtUtil;
import org.springframework.util.ResourceUtils;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;


@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${upload.path:./images/avatar}")
    private String uploadPath;

    @PostMapping("/login")
    public Result<?> login(@RequestBody LoginDTO loginDTO) {
        try {
            User user = userService.login(loginDTO);
            String accessToken = jwtUtil.generateToken(user.getUserId());
            String refreshToken = jwtUtil.generateRefreshToken(user.getUserId());

            Map<String, Object> result = new HashMap<>();
            result.put("user", user);
            result.put("accessToken", accessToken);
            result.put("refreshToken", refreshToken);
            result.put("expiresIn", 604800);

            return Result.success(result);
        } catch (RuntimeException e) {
            e.printStackTrace();
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/register")
    public Result<?> register(@RequestBody RegisterDTO registerDTO) {
        try {
            User user = userService.register(registerDTO);
            return Result.success(user);
        } catch (RuntimeException e) {
            e.printStackTrace();
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/info/current")
    public Result<?> getCurrentUserInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();

        User user = userService.getUserById(userId);
        return Result.success(user);
    }

    @PostMapping("/avatar")
    public Result<?> uploadAvatar(@RequestParam("avatar") MultipartFile file) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null) {
                return Result.error("用户未登录");
            }
            String userId = authentication.getName();

            if (file.isEmpty()) {
                return Result.error("请选择要上传的文件");
            }

            String originalFilename = file.getOriginalFilename();
            String fileExtension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            } else {
                fileExtension = ".jpg";
            }
            String fileName = UUID.randomUUID().toString() + fileExtension;

            String projectRootPath = System.getProperty("user.dir");
            String uploadDirPath = projectRootPath + File.separator + "src" + File.separator +
                    "main" + File.separator + "resources" + File.separator + "static" +
                    File.separator + "images" + File.separator + "avatar";

            Path uploadDir = Paths.get(uploadDirPath);
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            Path filePath = uploadDir.resolve(fileName);
            Files.copy(file.getInputStream(), filePath);

            String avatarUrl = "/images/avatar/" + fileName;
            User user = userService.updateAvatar(userId, avatarUrl);

            Map<String, Object> result = new HashMap<>();
            result.put("avatarUrl", avatarUrl);
            result.put("user", user);

            return Result.success(result);
        } catch (IOException e) {
            e.printStackTrace();
            return Result.error("上传失败：" + e.getMessage());
        } catch (RuntimeException e) {
            e.printStackTrace();
            return Result.error(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("上传失败，请重试");
        }
    }

    @PostMapping("/refresh-token")
    public Result<?> refreshToken(@RequestBody Map<String, String> request) {
        try {
            String refreshToken = request.get("refreshToken");
            if (refreshToken == null) {
                return Result.error("缺少 refreshToken");
            }

            if (!jwtUtil.validateToken(refreshToken)) {
                return Result.error("无效的 refreshToken");
            }

            String userId = jwtUtil.getUserIdFromToken(refreshToken);
            if (userId == null) {
                return Result.error("无法获取用户信息");
            }

            String newAccessToken = jwtUtil.generateToken(userId);
            String newRefreshToken = jwtUtil.generateRefreshToken(userId);

            Map<String, Object> result = new HashMap<>();
            result.put("accessToken", newAccessToken);
            result.put("refreshToken", newRefreshToken);
            result.put("expiresIn", 604800);

            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("刷新 token 失败");
        }
    }

    @GetMapping("/{id}")
    public Result<?> getUserById(@PathVariable String id) {
        try {
            User user = userService.getUserById(id);
            if (user == null) {
                return Result.error("用户不存在");
            }
            return Result.success(user);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取用户信息失败");
        }
    }

    // ==================== 用户自己更新资料 ====================

    @PutMapping("/profile")
    public Result<?> updateMyProfile(@RequestBody Map<String, Object> updates) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null) {
                return Result.error("用户未登录");
            }
            String userId = authentication.getName();

            User user = userService.getUserById(userId);
            if (user == null) {
                return Result.error("用户不存在");
            }

            // 只允许修改以下字段
            if (updates.containsKey("username")) user.setUsername((String) updates.get("username"));
            if (updates.containsKey("bio")) user.setBio((String) updates.get("bio"));
            if (updates.containsKey("gender")) user.setGender((String) updates.get("gender"));
            if (updates.containsKey("location")) user.setLocation((String) updates.get("location"));
            if (updates.containsKey("phoneNumber")) user.setPhoneNumber((String) updates.get("phoneNumber"));
            if (updates.containsKey("email")) user.setEmail((String) updates.get("email"));

            User updatedUser = userService.updateUser(userId, user);
            return Result.success(updatedUser);
        } catch (RuntimeException e) {
            e.printStackTrace();
            return Result.error(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("更新资料失败");
        }
    }

    @GetMapping("/info/current-with-stats")
    public Result<?> getCurrentUserInfoWithStats() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();

        User user = userService.getUserWithStats(userId);
        return Result.success(user);
    }

    // ==================== 管理员用户管理接口 ====================

    @GetMapping("/admin/list")
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

    @GetMapping("/admin/{userId}")
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

    @PutMapping("/admin/{userId}")
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

    @DeleteMapping("/admin/{userId}")
    public Result<?> deleteUser(@PathVariable String userId) {
        try {
            userService.deleteUser(userId);
            return Result.success(null);
        } catch (RuntimeException e) {
            e.printStackTrace();
            return Result.error(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("删除用户失败");
        }
    }

    @PutMapping("/admin/{userId}/status")
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