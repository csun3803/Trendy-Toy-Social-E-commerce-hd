package com.example.trendytoysocialecommercehd.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.util.Date;

@Data
@TableName("user_interaction")
public class UserInteraction {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("interaction_id")
    private String interactionId;

    @TableField("user_id")
    private String userId;

    @TableField("target_type")
    private String targetType;

    @TableField("target_id")
    private String targetId;

    @TableField("action_type")
    private String actionType;

    private String status;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private Date createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private Date updatedAt;
}
