-- 智能客服聊天记录表
CREATE TABLE IF NOT EXISTS `chat_message` (
    `message_id` VARCHAR(64) NOT NULL COMMENT '消息ID',
    `user_id` VARCHAR(64) NOT NULL COMMENT '用户ID',
    `session_id` VARCHAR(64) NOT NULL COMMENT '会话ID',
    `role` VARCHAR(20) NOT NULL COMMENT '角色: user/assistant',
    `content` TEXT NOT NULL COMMENT '消息内容',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`message_id`),
    INDEX `idx_user_session` (`user_id`, `session_id`),
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能客服聊天记录';
