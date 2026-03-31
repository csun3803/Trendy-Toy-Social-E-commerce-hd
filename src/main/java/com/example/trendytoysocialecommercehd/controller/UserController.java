package com.example.trendytoysocialecommercehd.controller;

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
            result.put("expiresIn", 1800); // 30分钟，单位秒

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
            System.out.println("开始上传头像...");
            System.out.println("文件大小: " + file.getSize());
            System.out.println("文件原始名称: " + file.getOriginalFilename());
            System.out.println("文件类型: " + file.getContentType());

            // 获取当前用户
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null) {
                System.out.println("认证信息为空");
                return Result.error("用户未登录");
            }
            String userId = authentication.getName();
            System.out.println("用户ID: " + userId);

            // 检查文件
            if (file.isEmpty()) {
                System.out.println("文件为空");
                return Result.error("请选择要上传的文件");
            }

            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String fileExtension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            } else {
                fileExtension = ".jpg";
            }
            String fileName = UUID.randomUUID().toString() + fileExtension;
            System.out.println("生成的文件名: " + fileName);

            // 获取项目根目录
            String projectRootPath = System.getProperty("user.dir");
            System.out.println("项目根目录: " + projectRootPath);

            // 构建绝对路径
            String uploadDirPath = projectRootPath + File.separator + "src" + File.separator + "main" +
                    File.separator + "resources" + File.separator + "static" +
                    File.separator + "images" + File.separator + "avatar";
            System.out.println("上传目录: " + uploadDirPath);

            // 确保目录存在
            Path uploadDir = Paths.get(uploadDirPath);
            if (!Files.exists(uploadDir)) {
                System.out.println("目录不存在，正在创建...");
                Files.createDirectories(uploadDir);
                System.out.println("目录创建成功");
            }

            // 保存文件
            Path filePath = uploadDir.resolve(fileName);
            System.out.println("保存路径: " + filePath.toAbsolutePath());

            Files.copy(file.getInputStream(), filePath);
            System.out.println("文件保存成功");

            // 构建文件路径
            String avatarUrl = "/images/avatar/" + fileName;
            System.out.println("头像URL: " + avatarUrl);

            // 更新用户头像
            User user = userService.updateAvatar(userId, avatarUrl);
            System.out.println("用户头像更新成功");

            // 返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("avatarUrl", avatarUrl);
            result.put("user", user);

            System.out.println("上传完成，返回结果");
            return Result.success(result);
        } catch (IOException e) {
            System.out.println("IO异常:");
            e.printStackTrace();
            return Result.error("上传失败：" + e.getMessage());
        } catch (RuntimeException e) {
            System.out.println("运行时异常:");
            e.printStackTrace();
            return Result.error(e.getMessage());
        } catch (Exception e) {
            System.out.println("未知异常:");
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

            // 验证 refreshToken
            if (!jwtUtil.validateToken(refreshToken)) {
                return Result.error("无效的 refreshToken");
            }

            // 从 refreshToken 中获取 userId
            String userId = jwtUtil.getUserIdFromToken(refreshToken);
            if (userId == null) {
                return Result.error("无法获取用户信息");
            }

            // 生成新的 accessToken 和 refreshToken
            String newAccessToken = jwtUtil.generateToken(userId);
            String newRefreshToken = jwtUtil.generateRefreshToken(userId);

            Map<String, Object> result = new HashMap<>();
            result.put("accessToken", newAccessToken);
            result.put("refreshToken", newRefreshToken);
            result.put("expiresIn", 1800); // 30分钟，单位秒

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
}