package com.example.trendytoysocialecommercehd.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("shop_certification_file")
public class ShopCertificationFile {
    @TableId(type = IdType.AUTO)
    private Long fileId;

    private String shopId;
    private String fileType; // business_license, id_card_front, id_card_back, brand_authorization, trademark_cert
    private String fileUrl;
    private String fileName;
    private Long fileSize;
    private String fileFormat;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
