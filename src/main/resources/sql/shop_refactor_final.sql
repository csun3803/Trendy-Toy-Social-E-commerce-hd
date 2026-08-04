-- ============================================================
-- Shop 表重构最终迁移脚本 (shop_refactor_final.sql)
-- 整合 shop_refactor.sql + shop_application_refactor.sql
-- 并将文件相关字段从 shop 表迁移到 shop_certification_file 表
-- ============================================================
-- 步骤概览:
--   1.  创建 shop_finance 表
--   2.  迁移 shop -> shop_finance 数据
--   3.  创建 shop_config 表
--   4.  迁移 shop -> shop_config 数据
--   5.  创建 shop_certification_file 表 (NEW)
--   6.  备份旧 shop 表 -> shop_backup
--   7.  删除旧 shop 表
--   8.  创建新 shop 表 (合并 schema, 不含文件字段)
--   9.  迁移 shop_backup -> 新 shop 表 (仅共有字段)
--   10. 迁移 shop_backup 文件字段 -> shop_certification_file (NEW)
--   11. 删除 shop_backup
--   12. 删除 merchant_application 表
-- ============================================================


-- ============================================================
-- Step 1: 创建 shop_finance 表
-- ============================================================
CREATE TABLE IF NOT EXISTS shop_finance (
    finance_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    shop_id VARCHAR(50) NOT NULL,
    bank_name VARCHAR(100),
    bank_account VARCHAR(50),
    account_holder VARCHAR(50),
    branch_name VARCHAR(100),
    deposit_balance DECIMAL(12,2) DEFAULT 0,
    deposit_status VARCHAR(20) DEFAULT '未缴纳',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_shop_id (shop_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ============================================================
-- Step 2: 迁移 shop -> shop_finance 数据
-- ============================================================
INSERT INTO shop_finance (shop_id, bank_name, bank_account, account_holder, branch_name, deposit_balance, deposit_status)
SELECT shop_id, bank_name, bank_account, account_holder, branch_name, deposit_balance, deposit_status
FROM shop;


-- ============================================================
-- Step 3: 创建 shop_config 表
-- ============================================================
CREATE TABLE IF NOT EXISTS shop_config (
    config_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    shop_id VARCHAR(50) NOT NULL,
    platform_commission_rate DECIMAL(5,4) DEFAULT 0,
    tech_service_rate DECIMAL(5,4) DEFAULT 0,
    free_shipping_setting VARCHAR(50),
    authenticity_guarantee INT DEFAULT 1,
    fake_compensation VARCHAR(100),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_shop_id (shop_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ============================================================
-- Step 4: 迁移 shop -> shop_config 数据
-- ============================================================
INSERT INTO shop_config (shop_id, platform_commission_rate, tech_service_rate, free_shipping_setting, authenticity_guarantee, fake_compensation)
SELECT shop_id, platform_commission_rate, tech_service_rate, free_shipping_setting, authenticity_guarantee, fake_compensation
FROM shop;


-- ============================================================
-- Step 5: 创建 shop_certification_file 表 (NEW)
-- ============================================================
CREATE TABLE IF NOT EXISTS shop_certification_file (
    file_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    shop_id VARCHAR(50) NOT NULL,
    file_type VARCHAR(30) NOT NULL COMMENT '文件类型: business_license/id_card_front/id_card_back',
    file_url VARCHAR(500),
    file_name VARCHAR(200),
    file_size BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_shop_id (shop_id),
    UNIQUE KEY uk_shop_file_type (shop_id, file_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ============================================================
-- Step 6: 备份旧 shop 表 -> shop_backup
-- ============================================================
DROP TABLE IF EXISTS shop_backup;
CREATE TABLE shop_backup AS SELECT * FROM shop;


-- ============================================================
-- Step 7: 删除旧 shop 表
-- ============================================================
DROP TABLE IF EXISTS shop;


-- ============================================================
-- Step 8: 创建新 shop 表 (合并 schema, 不含文件字段)
-- 注: 不包含 id_card_file_id, business_license_file_id, license_image,
--      id_card_front, id_card_back - 这些字段已迁移到 shop_certification_file
-- ============================================================
CREATE TABLE shop (
    shop_id VARCHAR(50) PRIMARY KEY,
    shop_name VARCHAR(100),
    shop_cover VARCHAR(255),
    shop_type VARCHAR(50),
    business_entity_type VARCHAR(50),
    legal_person_name VARCHAR(50),
    unified_social_credit_code VARCHAR(50),
    business_license_expiry VARCHAR(20),
    registered_capital VARCHAR(50),
    establishment_date DATE,
    business_scope TEXT,
    registered_address VARCHAR(255),
    shop_intro TEXT,
    main_categories VARCHAR(255),
    main_ips VARCHAR(255),
    customer_service_phone VARCHAR(20),
    customer_service_email VARCHAR(100),
    follower_count INT DEFAULT 0,
    product_count INT DEFAULT 0,
    shop_rating DECIMAL(3,2) DEFAULT 5.00,
    refund_rate DECIMAL(5,4) DEFAULT 0,
    shop_status VARCHAR(20),
    business_status VARCHAR(20),
    audit_status VARCHAR(20),
    audit_notes VARCHAR(255),
    audited_at DATETIME,
    auditor_id VARCHAR(50),
    audit_round INT,
    user_id VARCHAR(50),
    contact_person VARCHAR(50),
    contact_phone VARCHAR(20),
    contact_email VARCHAR(100),
    legal_person_id_card_number VARCHAR(50) COMMENT '法人身份证号码',
    legal_person_id_card_expiry VARCHAR(20) COMMENT '法人身份证有效期',
    return_address_province VARCHAR(50),
    return_address_city VARCHAR(50),
    return_address_district VARCHAR(50),
    return_address_detail VARCHAR(200),
    return_address_contact VARCHAR(50),
    return_address_phone VARCHAR(20),
    subject_type INT DEFAULT NULL COMMENT '主体类型: 0个人 1个体户 2企业',
    apply_sn VARCHAR(50) DEFAULT NULL COMMENT '申请单号',
    pending_data TEXT DEFAULT NULL COMMENT '待审核身份变更JSON',
    has_brand INT DEFAULT 0 COMMENT '是否有品牌: 0否 1是',
    brand_authorization_letter VARCHAR(500) COMMENT '品牌授权书文件ID',
    trademark_registration_cert VARCHAR(500) COMMENT '商标注册证文件ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ============================================================
-- Step 9: 迁移 shop_backup -> 新 shop 表 (仅共有字段)
-- 仅迁移 shop_backup 中存在的字段, 其余字段保持默认值
-- ============================================================
INSERT INTO shop (
    shop_id, shop_name, shop_cover, shop_type, business_entity_type,
    legal_person_name, unified_social_credit_code, business_license_expiry,
    registered_capital, establishment_date, business_scope, registered_address,
    shop_intro, main_categories, main_ips, customer_service_phone,
    customer_service_email, follower_count, product_count, shop_rating,
    refund_rate, shop_status, business_status, audit_status, audit_notes,
    audited_at, auditor_id, audit_round
)
SELECT
    shop_id, shop_name, shop_cover, shop_type, business_entity_type,
    legal_person_name, unified_social_credit_code, business_license_expiry,
    registered_capital, establishment_date, business_scope, registered_address,
    shop_intro, main_categories, main_ips, customer_service_phone,
    customer_service_email, follower_count, product_count, shop_rating,
    refund_rate, shop_status, business_status, audit_status, audit_notes,
    audited_at, auditor_id, audit_round
FROM shop_backup;


-- ============================================================
-- Step 10: 迁移 shop_backup 文件字段 -> shop_certification_file (NEW)
-- 从 shop_backup 提取 license_image, id_card_front, id_card_back
-- ============================================================
INSERT INTO shop_certification_file (shop_id, file_type, file_url)
SELECT shop_id, 'business_license', license_image FROM shop_backup
WHERE license_image IS NOT NULL AND license_image != '';

INSERT INTO shop_certification_file (shop_id, file_type, file_url)
SELECT shop_id, 'id_card_front', id_card_front FROM shop_backup
WHERE id_card_front IS NOT NULL AND id_card_front != '';

INSERT INTO shop_certification_file (shop_id, file_type, file_url)
SELECT shop_id, 'id_card_back', id_card_back FROM shop_backup
WHERE id_card_back IS NOT NULL AND id_card_back != '';


-- ============================================================
-- Step 11: 删除 shop_backup
-- ============================================================
DROP TABLE IF EXISTS shop_backup;


-- ============================================================
-- Step 12: 删除 merchant_application 表
-- ============================================================
DROP TABLE IF EXISTS merchant_application;
