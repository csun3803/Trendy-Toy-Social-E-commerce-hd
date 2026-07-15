package com.example.trendytoysocialecommercehd.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("shop_certification_file")
public class ShopCertificationFile {
    @TableId(value = "file_id", type = IdType.INPUT)
    private String fileId;

    private String shopId;
    private String fileType;
    private String fileName;
    private String filePath;
    private String fileUrl;
    private Long fileSize;
    private String fileFormat;
    private LocalDateTime uploadedAt;
    private String uploadedBy;
    private String auditStatus;
    private String auditNotes;
}