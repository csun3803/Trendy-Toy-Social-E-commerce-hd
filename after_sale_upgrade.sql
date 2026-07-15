-- After-sale table upgrade: add platform intervention, return address, timeout fields
-- New statuses: PLATFORM_REVIEWING, PLATFORM_RESOLVED

ALTER TABLE after_sale
    ADD COLUMN return_address TEXT COMMENT 'Merchant-filled return address' AFTER return_tracking_number,
    ADD COLUMN timeout_auto_approve_time DATETIME COMMENT 'Auto-approve deadline if merchant does not respond' AFTER return_address,
    ADD COLUMN platform_intervention_reason VARCHAR(500) COMMENT 'User reason for requesting platform intervention' AFTER timeout_auto_approve_time,
    ADD COLUMN platform_intervention_time DATETIME COMMENT 'Time user requested platform intervention' AFTER platform_intervention_reason,
    ADD COLUMN platform_admin_id VARCHAR(255) COMMENT 'Platform admin id who handled arbitration' AFTER platform_intervention_time,
    ADD COLUMN platform_arbitration_result VARCHAR(50) COMMENT 'Arbitration result: USER or SELLER' AFTER platform_admin_id,
    ADD COLUMN platform_arbitration_reason VARCHAR(500) COMMENT 'Platform arbitration reason' AFTER platform_arbitration_result,
    ADD COLUMN platform_arbitration_time DATETIME COMMENT 'Platform arbitration time' AFTER platform_arbitration_reason;

ALTER TABLE after_sale
    ADD INDEX idx_platform_intervention (after_sale_status, platform_intervention_time);
