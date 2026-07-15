package com.example.trendytoysocialecommercehd.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

/**
 * 盲盒排队实体
 */
@Data
@TableName("blind_box_queue")
public class BlindBoxQueue {

    @TableId(value = "queue_id", type = IdType.INPUT)
    private String queueId;

    /** 关联的抽盒机ID */
    private String machineId;

    /** 用户ID */
    private String userId;

    /** 队列位置 */
    private Integer queuePosition;

    /** 状态: WAITING/ACTIVE/LEFT */
    private String status;

    /** 加入时间 */
    private Date joinedAt;

    /** 离开时间 */
    private Date leftAt;
}
