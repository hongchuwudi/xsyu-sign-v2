---
name: xsyu-storage-media
description: Modify or troubleshoot XSYU Sign Markdown image upload, Aliyun OSS storage, media metadata, announcement asset references, draft tokens, and orphan cleanup. Use for repository media/OSS work, not generic file uploads.
---

# XSYU Sign media and Aliyun OSS

## Current implementation

- Upload endpoint: `POST /admin/assets/images`, protected by administrator authentication.
- Storage abstraction: `storage/ObjectStorageService` with `AliyunOssStorageService` implementation.
- Metadata service: `MediaAssetServiceImpl`.
- Tables: `media_asset` and `content_asset_ref`.
- Announcement Markdown association is derived by parsing Markdown AST; do not replace it with regex parsing.
- Frontend announcement editor uploads pasted, dropped, or selected images and inserts the returned public URL.

## Runtime configuration

Configuration lives under `xsyu.storage` and must be injected through environment/external config:

- `ALIYUN_OSS_ACCESS_KEY_ID`
- `ALIYUN_OSS_ACCESS_KEY_SECRET`
- `ALIYUN_OSS_ENDPOINT`
- `ALIYUN_OSS_BUCKET`
- `ALIYUN_OSS_PUBLIC_BASE_URL`
- `ALIYUN_OSS_OBJECT_PREFIX`
- `ALIYUN_OSS_MAX_FILE_SIZE`
- `ALIYUN_OSS_CLEANUP_CRON`

Never place AccessKeys in Git, frontend code, Dockerfile layers, committed YAML, logs, or error responses.

The current default public base URL is the bucket HTTPS URL `https://hc-base.oss-cn-beijing.aliyuncs.com`. Upload endpoint may use the Beijing internal OSS endpoint on a Beijing ECS, but browser URLs must remain publicly reachable HTTPS URLs.

## Object and database rules

- Object key format uses prefix + date + UUID + validated extension, for example `xsyu-sign/media/2026/09/18/<uuid>.webp`.
- Do not store image binary data in MySQL.
- Record object key, public URL, original name, detected MIME, size, SHA-256 hash, state, draft token, uploader, and timestamps.
- `media_asset.created_by` is `BIGINT UNSIGNED`, matching `user.id` exactly.
- `content_asset_ref` uniquely identifies `(biz_type, biz_id, asset_id)`; current business type includes `ANNOUNCEMENT`.

## Lifecycle

- Upload begins as `TEMP`, optionally associated with a new-announcement `draftToken`.
- Saving/updating an announcement parses its Markdown image nodes and creates/retains references; referenced assets become `ACTIVE`.
- Removing a reference or deleting content marks an unreferenced asset `ORPHAN`; do not immediately delete the OSS object.
- Cleanup deletes unreferenced `TEMP` assets older than 24 hours and unreferenced `ORPHAN` assets older than 7 days.
- Delete the OSS object and metadata consistently; log cleanup failures without exposing credentials.

## Upload validation

- Default maximum size is 10 MB.
- Allow JPEG, PNG, GIF, and WebP.
- Reject SVG.
- Validate file signatures/real type, not only extension or browser-provided `Content-Type`.
- Sanitize filenames for metadata/display and never incorporate an untrusted filename into the object key.

## Main files

- `xsyu-sign-server/src/main/java/com/hongchu/qqrobotsign/config/props/OssStorageProperties.java`
- `xsyu-sign-server/src/main/java/com/hongchu/qqrobotsign/storage/`
- `xsyu-sign-server/src/main/java/com/hongchu/qqrobotsign/service/impl/MediaAssetServiceImpl.java`
- `xsyu-sign-server/src/main/java/com/hongchu/qqrobotsign/service/impl/AnnouncementServiceImpl.java`
- `xsyu-sign-server/migrations/add_media_asset_tables.sql`
- `xsyu-sign-web/src/views/admin/announcement/AnnouncementView.vue`

## Deployment

Database migrations are not automatically applied by Flyway/Liquibase. Ensure media tables exist before deploying code that writes them. Verify OSS RAM policy is limited to necessary object operations under the configured bucket/prefix; do not grant RAM or bucket administration permissions.
