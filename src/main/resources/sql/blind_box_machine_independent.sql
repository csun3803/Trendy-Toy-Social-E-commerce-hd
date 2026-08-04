-- ============================================================
-- 抽盒机独立模式迁移脚本（完全清除旧数据，全面适配新架构）
-- 将抽盒机从商城数据独立出来，直接引用图鉴 series 表
-- 商城和抽盒机都引用图鉴但业务独立
-- ============================================================

-- 1. 清空旧数据（抽盒机相关表全部清空，从头来过）
TRUNCATE TABLE `blind_box_draw_record`;
TRUNCATE TABLE `blind_box_slot`;
TRUNCATE TABLE `blind_box_set`;
TRUNCATE TABLE `blind_box_machine_variant`;
TRUNCATE TABLE `blind_box_machine`;

-- 2. 扩展抽盒机主表：新增 series_id / set_count / hidden_count 字段，删除 sale_series_id
ALTER TABLE `blind_box_machine`
  ADD COLUMN `series_id` VARCHAR(64) NOT NULL COMMENT '关联的图鉴系列ID' AFTER `shop_id`,
  ADD COLUMN `set_count` INT NOT NULL DEFAULT 1 COMMENT '套数（商家配置）' AFTER `series_id`,
  ADD COLUMN `hidden_count` INT NOT NULL DEFAULT 0 COMMENT '隐藏款总数量（商家配置）' AFTER `set_count`,
  DROP COLUMN `sale_series_id`,
  ADD INDEX `idx_series_id` (`series_id`);

-- 3. 扩展盲盒槽位表：新增 set_id / variant_name / variant_image / variant_type，删除 sale_variant_id，修改唯一索引
ALTER TABLE `blind_box_slot`
  ADD COLUMN `set_id` VARCHAR(64) DEFAULT NULL COMMENT '关联的套盒ID' AFTER `machine_id`,
  ADD COLUMN `variant_name` VARCHAR(255) DEFAULT NULL COMMENT '缓存款式名称（创建时从图鉴复制）' AFTER `variant_id`,
  ADD COLUMN `variant_image` TEXT DEFAULT NULL COMMENT '缓存款式图片（创建时从图鉴复制）' AFTER `variant_name`,
  ADD COLUMN `variant_type` VARCHAR(20) DEFAULT NULL COMMENT '款式类型: regular常规/hidden隐藏' AFTER `variant_image`,
  DROP COLUMN `sale_variant_id`,
  DROP INDEX `uk_machine_slot_no`,
  ADD UNIQUE KEY `uk_machine_set_slot` (`machine_id`, `set_id`, `slot_no`),
  ADD INDEX `idx_set_id` (`set_id`);

-- 4. 抽盒记录表：删除 sale_variant_id
ALTER TABLE `blind_box_draw_record`
  DROP COLUMN `sale_variant_id`;

-- 5. 暂存柜表：删除 sale_variant_id
ALTER TABLE `blind_box_storage`
  DROP COLUMN `sale_variant_id`;

-- 6. 删除不再需要的 blind_box_machine_variant 表（独立模式不再使用款式覆盖配置）
DROP TABLE IF EXISTS `blind_box_machine_variant`;

-- ============================================================
-- 独立模式架构说明：
-- 1. 抽盒机创建流程：选择图鉴系列 → 设置单抽价格 → 配置套数/隐藏款数 → 生成盒子 → 上架
-- 2. 每套包含该系列所有普通款式各1个
-- 3. hiddenCount 个隐藏款随机分配到所有套盒中，替换普通款
-- 4. 隐藏款概率 = hiddenCount / 总盒数（系统自动计算）
-- 5. 盒子创建时缓存款式信息（name/image/type），不再依赖运行时查询 sale_variant
-- 6. 用户抽盒只能从可售盒子（AVAILABLE slot）中随机抽取
-- 7. 支付成功后盒子状态变为已售出（SOLD），不再扣减 sale_variant 库存
-- ============================================================
