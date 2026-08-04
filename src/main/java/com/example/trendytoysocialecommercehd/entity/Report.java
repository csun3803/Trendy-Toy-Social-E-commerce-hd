package com.example.trendytoysocialecommercehd.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.util.Date;

@Data
@TableName("report")
public class Report {
    @TableId(type = IdType.ASSIGN_UUID)
    private String reportId;
    private String reporterId;
    private String targetType;
    private String targetId;
    private String reason;
    private String status;
    private String resolvedBy;
    private Date resolvedAt;
    private String resolveNotes;
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private Date createdAt;
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private Date updatedAt;

    @TableField(exist = false)
    private ReporterInfo reporterInfo;

    @Data
    public static class ReporterInfo {
        private String userId;
        private String username;
        private String avatarUrl;
    }
}
