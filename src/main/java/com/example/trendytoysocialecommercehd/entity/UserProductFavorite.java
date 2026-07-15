package com.example.trendytoysocialecommercehd.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user_product_favorite")
public class UserProductFavorite {
    @TableId(value = "favorite_id", type = IdType.INPUT)
    private String favoriteId;

    @TableField("user_id")
    private String userId;

    @TableField("product_id")
    private String productId;

    private String status;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}