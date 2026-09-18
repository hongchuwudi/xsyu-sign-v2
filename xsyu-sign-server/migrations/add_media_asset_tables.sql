CREATE TABLE IF NOT EXISTS `media_asset` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `object_key` VARCHAR(512) NOT NULL,
    `public_url` VARCHAR(512) NOT NULL,
    `original_name` VARCHAR(255) NOT NULL,
    `content_type` VARCHAR(100) NOT NULL,
    `file_size` BIGINT UNSIGNED NOT NULL,
    `file_hash` CHAR(64) NOT NULL,
    `status` ENUM('TEMP', 'ACTIVE', 'ORPHAN') NOT NULL DEFAULT 'TEMP',
    `draft_token` VARCHAR(64) DEFAULT NULL,
    `created_by` BIGINT UNSIGNED DEFAULT NULL,
    `orphaned_at` DATETIME DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_media_asset_object_key` (`object_key`),
    UNIQUE KEY `uk_media_asset_public_url` (`public_url`),
    KEY `idx_media_asset_cleanup` (`status`, `orphaned_at`, `created_at`),
    KEY `idx_media_asset_draft_token` (`draft_token`),
    KEY `idx_media_asset_created_by` (`created_by`),
    CONSTRAINT `fk_media_asset_created_by` FOREIGN KEY (`created_by`) REFERENCES `user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `content_asset_ref` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `biz_type` VARCHAR(50) NOT NULL,
    `biz_id` BIGINT NOT NULL,
    `asset_id` BIGINT NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_content_asset_ref` (`biz_type`, `biz_id`, `asset_id`),
    KEY `idx_content_asset_ref_asset` (`asset_id`),
    CONSTRAINT `fk_content_asset_ref_asset` FOREIGN KEY (`asset_id`) REFERENCES `media_asset` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
