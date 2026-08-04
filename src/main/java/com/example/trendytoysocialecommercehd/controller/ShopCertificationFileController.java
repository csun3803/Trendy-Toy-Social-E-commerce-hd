package com.example.trendytoysocialecommercehd.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.trendytoysocialecommercehd.common.Result;
import com.example.trendytoysocialecommercehd.entity.ShopCertificationFile;
import com.example.trendytoysocialecommercehd.service.ShopCertificationFileService;
import com.example.trendytoysocialecommercehd.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    @GetMapping("/{shopId}/files")
    @Operation(summary = "获取店铺的所有资质文件", description = "根据店铺ID获取该店铺上传的所有资质文件")
    public Result<List<ShopCertificationFile>> getShopFiles(@PathVariable String shopId) {
        try {
            LambdaQueryWrapper<ShopCertificationFile> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ShopCertificationFile::getShopId, shopId);
            List<ShopCertificationFile> files = fileService.list(wrapper);
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

            // 保存到数据库 (按 shopId + fileType 唯一)
            fileService.saveOrUpdateFile(shopId, fileType, fileUrl);

            // 查询保存后的记录并返回
            LambdaQueryWrapper<ShopCertificationFile> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ShopCertificationFile::getShopId, shopId);
            wrapper.eq(ShopCertificationFile::getFileType, fileType);
            ShopCertificationFile saved = fileService.getOne(wrapper);
            if (saved != null && originalFilename != null) {
                saved.setFileName(originalFilename);
                saved.setFileSize(file.getSize());
                fileService.updateById(saved);
            }

            return Result.success(saved);
        } catch (IOException e) {
            e.printStackTrace();
            return Result.error("上传失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{shopId}/files/{fileId}")
    @Operation(summary = "删除店铺资质文件", description = "根据文件ID删除指定的资质文件")
    public Result<?> deleteShopFile(@PathVariable String shopId, @PathVariable Long fileId) {
        try {
            ShopCertificationFile file = fileService.getById(fileId);
            if (file == null) {
                return Result.error("文件不存在");
            }
            if (!file.getShopId().equals(shopId)) {
                return Result.error("无权删除此文件");
            }
            fileService.removeById(fileId);
            return Result.success("删除成功");
        } catch (Exception e) {
            return Result.error("删除失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{shopId}/files/type/{fileType}")
    @Operation(summary = "删除指定类型的所有文件", description = "删除店铺指定类型的所有资质文件")
    public Result<?> deleteFilesByType(@PathVariable String shopId, @PathVariable String fileType) {
        try {
            fileService.deleteFile(shopId, fileType);
            return Result.success("删除成功");
        } catch (Exception e) {
            return Result.error("删除失败: " + e.getMessage());
        }
    }
}
