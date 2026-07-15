package com.example.trendytoysocialecommercehd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.trendytoysocialecommercehd.entity.ShopCertificationFile;
import com.example.trendytoysocialecommercehd.mapper.ShopCertificationFileMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ShopCertificationFileService extends ServiceImpl<ShopCertificationFileMapper, ShopCertificationFile> {

    public List<ShopCertificationFile> getFilesByShopId(String shopId) {
        LambdaQueryWrapper<ShopCertificationFile> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShopCertificationFile::getShopId, shopId);
        wrapper.orderByDesc(ShopCertificationFile::getUploadedAt);
        return this.list(wrapper);
    }

    public ShopCertificationFile getFileById(String fileId) {
        return this.getById(fileId);
    }

    public void saveFile(ShopCertificationFile file) {
        this.save(file);
    }

    public void deleteFile(String fileId) {
        this.removeById(fileId);
    }

    public void deleteFilesByShopIdAndType(String shopId, String fileType) {
        LambdaQueryWrapper<ShopCertificationFile> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShopCertificationFile::getShopId, shopId);
        wrapper.eq(ShopCertificationFile::getFileType, fileType);
        this.remove(wrapper);
    }
}