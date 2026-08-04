-- 统一商家状态(shop_status)和审核状态(audit_status)为英文表示
-- 前端通过映射函数展示中文

-- 1. 更新 shop 表的 shop_status 字段为英文
UPDATE shop SET shop_status = 'PENDING_OPERATIONS' WHERE shop_status = '待营业' OR (shop_status IS NULL AND audit_status = '待审核');
UPDATE shop SET shop_status = 'ACTIVE' WHERE shop_status = '正常营业';
UPDATE shop SET shop_status = 'SUSPENDED' WHERE shop_status = '暂停营业';
UPDATE shop SET shop_status = 'CLOSED' WHERE shop_status IN ('已关闭', '停业整顿');

-- 2. 更新 shop 表的 audit_status 字段为英文
UPDATE shop SET audit_status = 'PENDING' WHERE audit_status = '待审核';
UPDATE shop SET audit_status = 'APPROVED' WHERE audit_status = '已通过';
UPDATE shop SET audit_status = 'REJECTED' WHERE audit_status = '已拒绝';

-- 3. 更新 shop_admin 表的 audit_status 字段（如有）
UPDATE shop_admin SET audit_status = 'PENDING' WHERE audit_status = '待审核';
UPDATE shop_admin SET audit_status = 'APPROVED' WHERE audit_status = '已通过';
UPDATE shop_admin SET audit_status = 'REJECTED' WHERE audit_status = '已拒绝';

-- 4. 更新 shop 表的 business_status 字段为英文
UPDATE shop SET business_status = 'OPERATING' WHERE business_status = '营业中';
UPDATE shop SET business_status = 'CLOSED' WHERE business_status = '已关闭';
UPDATE shop SET business_status = 'SUSPENDED' WHERE business_status = '暂停营业';

-- 验证
SELECT shop_id, shop_status, audit_status, business_status FROM shop LIMIT 20;

-- 5. 删除 shop 表的 business_entity_type 字段（不再使用，主体类型由 subjectType 表示）
ALTER TABLE shop DROP COLUMN business_entity_type;

-- 6. 给 shop_certification_file 表添加 file_format 字段
-- 用于存储文件扩展名（jpg/pdf 等），便于在审核页显示文件详细信息
-- 如果字段已存在会报错，可忽略
ALTER TABLE shop_certification_file ADD COLUMN file_format VARCHAR(20) NULL COMMENT '文件格式（扩展名）' AFTER file_size;

-- 7. 将已存在的品牌授权书/商标注册证 URL 从 shop 表迁移到 shop_certification_file 表
-- 这样审核页和详情页就能统一从 shop_certification_file 表读取所有资质文件
INSERT INTO shop_certification_file (shop_id, file_type, file_url, file_name, file_format, created_at, updated_at)
SELECT shop_id, 'brand_authorization', brand_authorization_letter, '品牌授权书',
       SUBSTRING_INDEX(brand_authorization_letter, '.', -1), NOW(), NOW()
FROM shop
WHERE brand_authorization_letter IS NOT NULL AND brand_authorization_letter != ''
  AND NOT EXISTS (
    SELECT 1 FROM shop_certification_file scf
    WHERE scf.shop_id = shop.shop_id AND scf.file_type = 'brand_authorization'
  );

INSERT INTO shop_certification_file (shop_id, file_type, file_url, file_name, file_format, created_at, updated_at)
SELECT shop_id, 'trademark_cert', trademark_registration_cert, '商标注册证',
       SUBSTRING_INDEX(trademark_registration_cert, '.', -1), NOW(), NOW()
FROM shop
WHERE trademark_registration_cert IS NOT NULL AND trademark_registration_cert != ''
  AND NOT EXISTS (
    SELECT 1 FROM shop_certification_file scf
    WHERE scf.shop_id = shop.shop_id AND scf.file_type = 'trademark_cert'
  );
