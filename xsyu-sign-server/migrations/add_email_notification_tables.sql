CREATE TABLE IF NOT EXISTS `email_template` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(100) NOT NULL,
    `subject` VARCHAR(255) NOT NULL,
    `content` TEXT NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_email_template_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `user_group` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(100) NOT NULL,
    `description` VARCHAR(500) DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_group_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `user_group_member` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `group_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_group_user` (`group_id`, `user_id`),
    KEY `idx_group_member_user` (`user_id`),
    CONSTRAINT `fk_group_member_group` FOREIGN KEY (`group_id`) REFERENCES `user_group` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_group_member_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `email_notification_task` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `subject` VARCHAR(255) NOT NULL,
    `content` TEXT NOT NULL,
    `status` VARCHAR(32) NOT NULL,
    `enabled` TINYINT(1) NOT NULL DEFAULT 1,
    `scheduled_at` DATETIME DEFAULT NULL,
    `started_at` DATETIME DEFAULT NULL,
    `completed_at` DATETIME DEFAULT NULL,
    `total_count` INT NOT NULL DEFAULT 0,
    `success_count` INT NOT NULL DEFAULT 0,
    `failure_count` INT NOT NULL DEFAULT 0,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_email_task_due` (`status`, `enabled`, `scheduled_at`),
    KEY `idx_email_task_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `email_task_recipient` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `task_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `username` VARCHAR(100) NOT NULL,
    `name` VARCHAR(100) DEFAULT NULL,
    `email` VARCHAR(255) NOT NULL,
    `status` VARCHAR(32) NOT NULL,
    `error_message` VARCHAR(1000) DEFAULT NULL,
    `sent_at` DATETIME DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_task_recipient_user` (`task_id`, `user_id`),
    KEY `idx_email_recipient_task_status` (`task_id`, `status`),
    CONSTRAINT `fk_email_recipient_task` FOREIGN KEY (`task_id`) REFERENCES `email_notification_task` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
