-- 店铺表添加shop_logo字段
-- 执行日期: 2026-06-05

ALTER TABLE shop 
ADD COLUMN shop_logo VARCHAR(500) COMMENT '店铺logo' 
AFTER shop_cover;
