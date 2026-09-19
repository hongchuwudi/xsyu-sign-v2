-- 学校密码加密主密钥轮换（直接重置方案）
--
-- 执行前：
-- 1. 确认生产环境已设置新的 XSYU_STUDENT_PASSWORD_MASTER_KEY（至少 32 个字符）。
-- 2. 确认管理员本地密码已经是 PBKDF2 格式：password 文本以 310000: 开头。
-- 3. 备份数据库。
--
-- 执行后：
-- - 已有 JWS 不会被清空，当前会话和签到可继续使用至 JWS 失效。
-- - 所有用户需要通过学校密码重新登录一次，或在个人中心更新学校密码，
--   才能恢复自动 JWS 续签能力。

-- 部署前先检查管理员密码格式。结果必须全部为 OK；否则先在旧版本中登录一次管理员账号，
-- 让旧版本把管理员密码迁移为 PBKDF2，再继续部署。
SELECT `username`,
       CASE
           WHEN CONVERT(`password` USING utf8mb4) LIKE '310000:%' THEN 'OK'
           ELSE 'MUST_MIGRATE_BEFORE_DEPLOY'
       END AS admin_password_status
FROM `user`
WHERE `role` = 'ADMIN';

START TRANSACTION;

SELECT COUNT(*) AS passwords_to_reset
FROM `user`
WHERE `stu_password` IS NOT NULL;

UPDATE `user`
SET `stu_password` = NULL,
    `updated_at` = NOW()
WHERE `stu_password` IS NOT NULL;

SELECT ROW_COUNT() AS passwords_reset;

COMMIT;
