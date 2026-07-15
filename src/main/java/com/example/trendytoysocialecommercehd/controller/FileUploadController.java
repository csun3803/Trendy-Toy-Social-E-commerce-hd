package com.example.trendytoysocialecommercehd.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.trendytoysocialecommercehd.common.Result;
import com.example.trendytoysocialecommercehd.entity.ShopCertificationFile;
import com.example.trendytoysocialecommercehd.service.ShopCertificationFileService;
import com.example.trendytoysocialecommercehd.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@Tag(name = "文件上传", description = "通用文件上传接口")
public class FileUploadController {

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${upload.path:./images}")
    private String basePath;

    @PostMapping("/upload")
    @Operation(summary = "通用文件上传", description = "上传文件并返回访问URL")
    public Result<Map<String, String>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "Authorization", required = false) String token) {
        
        if (file.isEmpty()) {
            return Result.error("请选择要上传的文件");
        }

        try {
            String projectRootPath = System.getProperty("user.dir");
            String uploadDirPath = projectRootPath + File.separator + "src" + File.separator +
                    "main" + File.separator + "resources" + File.separator + "static" +
                    File.separator + "images" + File.separator + "upload";

            Path uploadDir = Paths.get(uploadDirPath);
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            String originalFilename = file.getOriginalFilename();
            String fileExtension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            } else {
                fileExtension = ".jpg";
            }

            String fileName = UUID.randomUUID().toString() + fileExtension;
            Path filePath = uploadDir.resolve(fileName);
            Files.copy(file.getInputStream(), filePath);

            String fileUrl = "/images/upload/" + fileName;

            Map<String, String> result = new HashMap<>();
            result.put("url", fileUrl);
            result.put("fileName", fileName);
            result.put("originalFilename", originalFilename);

            return Result.success(result);
        } catch (IOException e) {
            e.printStackTrace();
            return Result.error("上传失败: " + e.getMessage());
        }
    }
}
