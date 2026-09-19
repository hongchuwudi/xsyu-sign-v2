-- 统一使用 user.password 字段并完成学校密码主密钥轮换。
--
-- 最终字段规则：
-- - ADMIN：password 保存 PBKDF2 不可逆哈希。
-- - USER 密码登录：password 保存新主密钥 AES-GCM 密文。
-- - USER 短信/扫码登录：password 为 NULL。
--
-- 执行前：
-- 1. 确认生产环境已设置新的 XSYU_STUDENT_PASSWORD_MASTER_KEY（至少 32 个字符）。
-- 2. 确认管理员 password 是 PBKDF2 格式。
-- 3. 备份数据库。
-- 4. 停止旧版本应用，避免旧代码继续读写 stu_password。

SELECT `username`,
       CASE
           WHEN CONVERT(`password` USING utf8mb4) LIKE '310000:%' THEN 'OK'
           ELSE 'MUST_MIGRATE_BEFORE_DEPLOY'
       END AS admin_password_status
FROM `user`
WHERE `role` = 'ADMIN';

START TRANSACTION;

SELECT COUNT(*) AS legacy_user_passwords_to_reset
FROM `user`
WHERE `role` = 'USER'
  AND `password` IS NOT NULL;

UPDATE `user`
SET `password` = NULL,
    `updated_at` = NOW()
WHERE `role` = 'USER'
  AND `password` IS NOT NULL;

SELECT ROW_COUNT() AS legacy_user_passwords_reset;

COMMIT;

-- 旧字段已经不再被应用使用。MySQL 8.0 支持 DROP COLUMN。
ALTER TABLE `user`
    DROP COLUMN `stu_password`,
    MODIFY COLUMN `password` BLOB NULL
        COMMENT '统一密码字段：管理员PBKDF2哈希/普通用户AES-GCM学校密码/可空';

DESC `user`;
