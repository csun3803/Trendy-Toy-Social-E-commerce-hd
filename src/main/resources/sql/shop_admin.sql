-- 确保 shop_admin 表结构完整
CREATE TABLE IF NOT EXISTS `shop_admin` (
  `admin_id` VARCHAR(50) NOT NULL COMMENT '管理员ID/手机号',
  `shop_id` VARCHAR(50) DEFAULT NULL COMMENT '关联店铺ID',
  `password_hash` VARCHAR(255) NOT NULL COMMENT '密码哈希',
  `is_active` INT DEFAULT 1 COMMENT '是否启用(1启用/0禁用)',
  `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
  `last_operation_time` DATETIME DEFAULT NULL COMMENT '最后操作时间',
  `audit_status` VARCHAR(20) DEFAULT '待审核' COMMENT '审核状态',
  `audit_notes` VARCHAR(500) DEFAULT NULL COMMENT '审核备注',
  `audited_at` DATETIME DEFAULT NULL COMMENT '审核时间',
  `login_count` INT DEFAULT 0 COMMENT '登录次数',
  PRIMARY KEY (`admin_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商家管理员表';

-- 确保 platform_admin 表结构完整
CREATE TABLE IF NOT EXISTS `platform_admin` (
  `admin_id` VARCHAR(50) NOT NULL COMMENT '管理员ID',
  `employee_id` VARCHAR(50) DEFAULT NULL COMMENT '工号',
  `password_hash` VARCHAR(255) NOT NULL COMMENT '密码哈希',
  `admin_level` VARCHAR(20) DEFAULT NULL COMMENT '管理员级别',
  `department` VARCHAR(100) DEFAULT NULL COMMENT '部门',
  `position` VARCHAR(100) DEFAULT NULL COMMENT '职位',
  `management_scope` VARCHAR(500) DEFAULT NULL COMMENT '管理范围',
  `system_permissions` TEXT DEFAULT NULL COMMENT '系统权限',
  `data_permissions` TEXT DEFAULT NULL COMMENT '数据权限',
  `operation_permissions` TEXT DEFAULT NULL COMMENT '操作权限',
  `approval_permissions` TEXT DEFAULT NULL COMMENT '审批权限',
  `account_status` VARCHAR(20) DEFAULT 'active' COMMENT '账户状态',
  `activated_at` DATETIME DEFAULT NULL COMMENT '激活时间',
  `deactivated_at` DATETIME DEFAULT NULL COMMENT '停用时间',
  `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
  `last_operation_time` DATETIME DEFAULT NULL COMMENT '最后操作时间',
  PRIMARY KEY (`admin_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='平台管理员表';

-- 检查并添加 shop_admin 表缺少的列
-- 如果表已存在但缺少字段，逐个添加（忽略已存在的列错误）

-- MySQL 8.0+ 可以用存储过程安全添加列
DELIMITER //
DROP PROCEDURE IF EXISTS add_shop_admin_columns//
CREATE PROCEDURE add_shop_admin_columns()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'shop_admin' AND COLUMN_NAME = 'audit_status') THEN
        ALTER TABLE `shop_admin` ADD COLUMN `audit_status` VARCHAR(20) DEFAULT '待审核' COMMENT '审核状态';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'shop_admin' AND COLUMN_NAME = 'audit_notes') THEN
        ALTER TABLE `shop_admin` ADD COLUMN `audit_notes` VARCHAR(500) DEFAULT NULL COMMENT '审核备注';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'shop_admin' AND COLUMN_NAME = 'audited_at') THEN
        ALTER TABLE `shop_admin` ADD COLUMN `audited_at` DATETIME DEFAULT NULL COMMENT '审核时间';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'shop_admin' AND COLUMN_NAME = 'login_count') THEN
        ALTER TABLE `shop_admin` ADD COLUMN `login_count` INT DEFAULT 0 COMMENT '登录次数';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'shop_admin' AND COLUMN_NAME = 'is_active') THEN
        ALTER TABLE `shop_admin` ADD COLUMN `is_active` INT DEFAULT 1 COMMENT '是否启用';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'shop_admin' AND COLUMN_NAME = 'last_operation_time') THEN
        ALTER TABLE `shop_admin` ADD COLUMN `last_operation_time` DATETIME DEFAULT NULL COMMENT '最后操作时间';
    END IF;
END//
DELIMITER ;
CALL add_shop_admin_columns();
DROP PROCEDURE IF EXISTS add_shop_admin_columns;
