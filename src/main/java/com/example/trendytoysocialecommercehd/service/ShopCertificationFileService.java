package com.example.trendytoysocialecommercehd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.trendytoysocialecommercehd.dto.ShopApplyDTO;
import com.example.trendytoysocialecommercehd.entity.ShopCertificationFile;
import com.example.trendytoysocialecommercehd.mapper.ShopCertificationFileMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ShopCertificationFileService extends ServiceImpl<ShopCertificationFileMapper, ShopCertificationFile> {

    /**
     * 获取指定店铺的所有文件 URL 映射
     * 返回 map: {business_license: url, id_card_front: url, id_card_back: url}
     */
    public Map<String, String> getFileUrlsByShopId(String shopId) {
        Map<String, String> result = new HashMap<>();
        LambdaQueryWrapper<ShopCertificationFile> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShopCertificationFile::getShopId, shopId);
        List<ShopCertificationFile> files = this.list(wrapper);
        if (files != null) {
            for (ShopCertificationFile file : files) {
                if (file.getFileType() != null) {
                    result.put(file.getFileType(), file.getFileUrl());
                }
            }
        }
        return result;
    }

    /**
     * 保存或更新文件记录 (按 shopId + fileType 唯一)
     * 如果存在记录则更新 fileUrl, 否则插入新记录
     */
    public void saveOrUpdateFile(String shopId, String fileType, String fileUrl) {
        LambdaQueryWrapper<ShopCertificationFile> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShopCertificationFile::getShopId, shopId);
        wrapper.eq(ShopCertificationFile::getFileType, fileType);
        ShopCertificationFile existing = this.getOne(wrapper);
        if (existing != null) {
            existing.setFileUrl(fileUrl);
            this.updateById(existing);
        } else {
            ShopCertificationFile file = new ShopCertificationFile();
            file.setShopId(shopId);
            file.setFileType(fileType);
            file.setFileUrl(fileUrl);
            this.save(file);
        }
    }

    /**
     * 批量保存文件 URL (对 map 中每个 entry 调用 saveOrUpdateFile)
     */
    public void saveFilesFromMap(String shopId, Map<String, String> fileMap) {
        if (fileMap == null || fileMap.isEmpty()) {
            return;
        }
        for (Map.Entry<String, String> entry : fileMap.entrySet()) {
            saveOrUpdateFile(shopId, entry.getKey(), entry.getValue());
        }
    }

    /**
     * 删除指定店铺指定类型的文件
     */
    public void deleteFile(String shopId, String fileType) {
        LambdaQueryWrapper<ShopCertificationFile> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShopCertificationFile::getShopId, shopId);
        wrapper.eq(ShopCertificationFile::getFileType, fileType);
        this.remove(wrapper);
    }

    /**
     * 保存或更新文件记录（带详细信息）
     * 按 shopId + fileType 唯一，存在则更新，否则插入
     */
    public void saveOrUpdateFileDetail(String shopId, String fileType, String fileUrl,
                                        String fileName, Long fileSize, String fileFormat) {
        LambdaQueryWrapper<ShopCertificationFile> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShopCertificationFile::getShopId, shopId);
        wrapper.eq(ShopCertificationFile::getFileType, fileType);
        ShopCertificationFile existing = this.getOne(wrapper);
        LocalDateTime now = LocalDateTime.now();
        if (existing != null) {
            existing.setFileUrl(fileUrl);
            existing.setFileName(fileName);
            existing.setFileSize(fileSize);
            existing.setFileFormat(fileFormat);
            existing.setUpdatedAt(now);
            this.updateById(existing);
        } else {
            ShopCertificationFile file = new ShopCertificationFile();
            file.setShopId(shopId);
            file.setFileType(fileType);
            file.setFileUrl(fileUrl);
            file.setFileName(fileName);
            file.setFileSize(fileSize);
            file.setFileFormat(fileFormat);
            file.setCreatedAt(now);
            file.setUpdatedAt(now);
            this.save(file);
        }
    }

    /**
     * 批量保存文件详细信息
     * 对于 ShopApplyDTO.files 列表中的每个文件，保存到 shop_certification_file 表
     * 列表中未包含的旧文件类型会被删除
     */
    public void saveFileDetails(String shopId, List<ShopApplyDTO.FileDetail> files) {
        if (files == null || files.isEmpty()) {
            return;
        }
        // 收集本次提交涉及的所有 fileType
        java.util.Set<String> incomingTypes = new java.util.HashSet<>();
        for (ShopApplyDTO.FileDetail f : files) {
            if (f.getFileType() == null || f.getFileUrl() == null) continue;
            incomingTypes.add(f.getFileType());
            saveOrUpdateFileDetail(shopId, f.getFileType(), f.getFileUrl(),
                    f.getFileName(), f.getFileSize(), f.getFileFormat());
        }
        // 删除本次未提交的旧文件记录（避免残留）
        if (!incomingTypes.isEmpty()) {
            LambdaQueryWrapper<ShopCertificationFile> delWrapper = new LambdaQueryWrapper<>();
            delWrapper.eq(ShopCertificationFile::getShopId, shopId);
            delWrapper.notIn(ShopCertificationFile::getFileType, incomingTypes);
            this.remove(delWrapper);
        }
    }
}
