-- 修复数据库字符集排序规则冲突问题
-- 将所有表的字符集排序规则统一为 utf8mb4_unicode_ci

-- 检查表的当前排序规则
SELECT 
    TABLE_NAME,
    TABLE_COLLATION 
FROM INFORMATION_SCHEMA.TABLES 
WHERE TABLE_SCHEMA = DATABASE();

-- 检查列的当前排序规则
SELECT 
    TABLE_NAME,
    COLUMN_NAME,
    COLLATION_NAME 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = DATABASE() 
    AND COLLATION_NAME IS NOT NULL
    AND COLLATION_NAME != 'utf8mb4_unicode_ci'
ORDER BY TABLE_NAME, COLUMN_NAME;

-- =====================================================================
-- 修复方案 1: 统一所有表和列的排序规则为 utf8mb4_unicode_ci
-- =====================================================================

-- sale_series 表
ALTER TABLE sale_series CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- sale_variant 表
ALTER TABLE sale_variant CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- orders 表
ALTER TABLE orders CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- order_items 表
ALTER TABLE order_items CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- =====================================================================
-- 如果上面的方案不行，或者只想修复特定列，使用方案 2
-- =====================================================================

-- 方案 2: 只修复关联键的排序规则
-- ALTER TABLE sale_series MODIFY COLUMN sale_series_id VARCHAR(64) COLLATE utf8mb4_unicode_ci;
-- ALTER TABLE sale_variant MODIFY COLUMN sale_series_id VARCHAR(64) COLLATE utf8mb4_unicode_ci;
-- ALTER TABLE sale_variant MODIFY COLUMN sale_variant_id VARCHAR(64) COLLATE utf8mb4_unicode_ci;
-- ALTER TABLE orders MODIFY COLUMN order_id VARCHAR(64) COLLATE utf8mb4_unicode_ci;
-- ALTER TABLE order_items MODIFY COLUMN order_id VARCHAR(64) COLLATE utf8mb4_unicode_ci;
-- ALTER TABLE order_items MODIFY COLUMN product_id VARCHAR(64) COLLATE utf8mb4_unicode_ci;

-- 验证修复结果
SELECT 
    TABLE_NAME,
    TABLE_COLLATION 
FROM INFORMATION_SCHEMA.TABLES 
WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME IN ('sale_series', 'sale_variant', 'orders', 'order_items');
