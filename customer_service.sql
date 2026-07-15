-- 客服会话表
CREATE TABLE IF NOT EXISTS `customer_service_session` (
  `session_id` VARCHAR(50) NOT NULL COMMENT '会话ID',
  `user_id` VARCHAR(50) NOT NULL COMMENT '用户ID',
  `user_nickname` VARCHAR(100) DEFAULT NULL COMMENT '用户昵称',
  `last_message_content` VARCHAR(500) DEFAULT NULL COMMENT '最后一条消息内容',
  `last_message_time` DATETIME DEFAULT NULL COMMENT '最后消息时间',
  `unread_count` INT DEFAULT 0 COMMENT '未读消息数',
  `status` VARCHAR(20) DEFAULT '待处理' COMMENT '状态：待处理/处理中/已关闭',
  `source` VARCHAR(20) DEFAULT NULL COMMENT '来源：商品咨询/订单售后',
  `admin_id` VARCHAR(50) DEFAULT NULL COMMENT '处理的管理员ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`session_id`),
  KEY `idx_status` (`status`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_last_message_time` (`last_message_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='客服会话表';

-- 客服消息表
CREATE TABLE IF NOT EXISTS `customer_service_message` (
  `message_id` VARCHAR(50) NOT NULL COMMENT '消息ID',
  `session_id` VARCHAR(50) NOT NULL COMMENT '会话ID',
  `sender_type` VARCHAR(20) NOT NULL COMMENT '发送者类型：user/admin',
  `sender_id` VARCHAR(50) NOT NULL COMMENT '发送者ID',
  `content` TEXT NOT NULL COMMENT '消息内容',
  `message_type` VARCHAR(20) DEFAULT 'text' COMMENT '消息类型：text/image',
  `is_read` TINYINT DEFAULT 0 COMMENT '是否已读：0未读/1已读',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`message_id`),
  KEY `idx_session_id` (`session_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='客服消息表';
