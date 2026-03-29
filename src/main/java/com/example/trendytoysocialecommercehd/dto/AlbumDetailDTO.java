package com.example.trendytoysocialecommercehd.dto;

import lombok.Data;
import java.time.LocalDateTime;

// AlbumDetailDTO.java - 详情页数据
@Data
public class AlbumDetailDTO {
    private String albumId;
    private String ipName;
    private String shortDescription;
    private String copyrightOwner;
    private String logo;
    private LocalDateTime creationTime;
    private String creator;
    private String countryOrigin;
    private Boolean isHotIp;
    private String auditStatus;
}
