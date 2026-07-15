-- 用户行为记录表（供智能推荐算法使用）
-- 由Java后端在用户浏览/收藏/购买等行为发生时写入，或由前端直接上报给ai-service
CREATE TABLE IF NOT EXISTS `user_behavior` (
    `behavior_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '行为ID',
    `user_id` VARCHAR(64) NOT NULL COMMENT '用户ID',
    `behavior_type` VARCHAR(32) NOT NULL COMMENT '行为类型: BROWSE/FAVORITE/UNFAVORITE/PURCHASE/SEARCH/SHARE',
    `target_type` VARCHAR(32) NOT NULL COMMENT '目标类型: SERIES/PRODUCT/SHOP',
    `target_id` VARCHAR(64) NOT NULL COMMENT '目标ID',
    `weight` INT DEFAULT 1 COMMENT '行为权重(浏览=1,收藏=3,购买=5)',
    `behavior_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '行为发生时间',
    PRIMARY KEY (`behavior_id`),
    INDEX `idx_user_time` (`user_id`, `behavior_time`),
    INDEX `idx_target` (`target_type`, `target_id`),
    INDEX `idx_behavior_type` (`behavior_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户行为记录(用于AI推荐)';
