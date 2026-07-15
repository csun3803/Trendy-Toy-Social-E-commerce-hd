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
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/shop")
@Tag(name = "店铺资质文件", description = "店铺资质文件上传和查询接口")
public class ShopCertificationFileController {

    @Autowired
    private ShopCertificationFileService fileService;

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${upload.path:./images}")
    private String basePath;

    @GetMapping("/{shopId}/files")
    @Operation(summary = "获取店铺的所有资质文件", description = "根据店铺ID获取该店铺上传的所有资质文件")
    public Result<List<ShopCertificationFile>> getShopFiles(@PathVariable String shopId) {
        try {
            List<ShopCertificationFile> files = fileService.getFilesByShopId(shopId);
            return Result.success(files);
        } catch (Exception e) {
            return Result.error("获取文件失败: " + e.getMessage());
        }
    }

    @PostMapping("/{shopId}/files")
    @Operation(summary = "上传店铺资质文件", description = "上传店铺资质文件")
    public Result<ShopCertificationFile> uploadShopFile(
            @PathVariable String shopId,
            @RequestParam("fileType") String fileType,
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "Authorization", required = false) String token) {

        if (file.isEmpty()) {
            return Result.error("请选择要上传的文件");
        }

        try {
            String projectRootPath = System.getProperty("user.dir");
            String uploadDirPath = projectRootPath + File.separator + "src" + File.separator + "main" +
                    File.separator + "resources" + File.separator + "static" +
                    File.separator + "images" + File.separator + "shop-certification";

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

            String fileUrl = "/images/shop-certification/" + fileName;

            String uploadedBy = null;
            if (token != null && !token.isEmpty()) {
                String cleanToken = token.replace("Bearer ", "");
                if (jwtUtil.validateToken(cleanToken)) {
                    uploadedBy = jwtUtil.getUserIdFromToken(cleanToken);
                }
            }

            ShopCertificationFile certificationFile = new ShopCertificationFile();
            certificationFile.setFileId("file_" + System.currentTimeMillis());
            certificationFile.setShopId(shopId);
            certificationFile.setFileType(fileType);
            certificationFile.setFileName(originalFilename);
            certificationFile.setFilePath(filePath.toString());
            certificationFile.setFileUrl(fileUrl);
            certificationFile.setFileSize(file.getSize());
            certificationFile.setFileFormat(fileExtension);
            certificationFile.setUploadedAt(LocalDateTime.now());
            certificationFile.setUploadedBy(uploadedBy);
            certificationFile.setAuditStatus("待审核");

            fileService.saveFile(certificationFile);

            return Result.success(certificationFile);
        } catch (IOException e) {
            e.printStackTrace();
            return Result.error("上传失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{shopId}/files/{fileId}")
    @Operation(summary = "删除店铺资质文件", description = "根据文件ID删除指定的资质文件")
    public Result<?> deleteShopFile(@PathVariable String shopId, @PathVariable String fileId) {
        try {
            ShopCertificationFile file = fileService.getFileById(fileId);
            if (file == null) {
                return Result.error("文件不存在");
            }
            if (!file.getShopId().equals(shopId)) {
                return Result.error("无权删除此文件");
            }
            fileService.deleteFile(fileId);
            return Result.success("删除成功");
        } catch (Exception e) {
            return Result.error("删除失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{shopId}/files/type/{fileType}")
    @Operation(summary = "删除指定类型的所有文件", description = "删除店铺指定类型的所有资质文件")
    public Result<?> deleteFilesByType(@PathVariable String shopId, @PathVariable String fileType) {
        try {
            fileService.deleteFilesByShopIdAndType(shopId, fileType);
            return Result.success("删除成功");
        } catch (Exception e) {
            return Result.error("删除失败: " + e.getMessage());
        }
    }
}