package com.example.trendytoysocialecommercehd.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("TECHNICAL_IP_ALBUM")
public class TechnicalIpAlbum {

    @TableId(value = "album_id", type = IdType.INPUT)
    private String albumId;

    @TableField("ip_name")
    private String ipName;

    @TableField("short_description")
    private String shortDescription;

    @TableField("copyright_owner")
    private String copyrightOwner;

    @TableField("logo")
    private String logo;

    @TableField("creation_time")
    private LocalDateTime creationTime;

    @TableField("creator")
    private String creator;

    @TableField("country_origin")
    private String countryOrigin;

    @TableField("is_hot_ip")
    private Boolean isHotIp;

    @TableField("audit_status")
    private String auditStatus;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
