ALTER TABLE `operation_log`
    MODIFY COLUMN `detail` TEXT NULL COMMENT '详细信息',
    ADD COLUMN `user_id` BIGINT UNSIGNED NULL COMMENT '目标用户ID快照' AFTER `operator`,
    ADD COLUMN `username` VARCHAR(64) NULL COMMENT '目标学号或用户名快照' AFTER `user_id`,
    ADD COLUMN `user_name` VARCHAR(100) NULL COMMENT '目标用户姓名快照' AFTER `username`,
    ADD COLUMN `action_method` VARCHAR(32) NULL COMMENT 'PASSWORD/SMS/QR/MANUAL/ADMIN等操作方式' AFTER `user_name`,
    ADD COLUMN `request_uri` VARCHAR(255) NULL COMMENT '触发日志的请求路径' AFTER `ip`,
    ADD INDEX `idx_operation_log_username_created_at` (`username`, `created_at`),
    ADD INDEX `idx_operation_log_type_result_created_at` (`log_type`, `result`, `created_at`),
    ADD INDEX `idx_operation_log_method_created_at` (`action_method`, `created_at`);
