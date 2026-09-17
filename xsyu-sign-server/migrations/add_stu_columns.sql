-- 用户体系重构：学号即用户名（username = 学号，唯一标识）
-- stu_password: CAS 密码（AES-GCM 可逆加密，JWS 自动续签用；短信/扫码登录用户为 NULL）
-- phone: 绑定手机号（短信登录时记录）
-- role: USER / ADMIN
-- password: 改为可空（仅管理员本地登录使用 PBKDF2 哈希）

USE xsyu;

ALTER TABLE user
ADD COLUMN stu_password BLOB NULL COMMENT 'CAS密码（AES-GCM加密，可空）' AFTER password,
ADD COLUMN phone VARCHAR(20) NULL COMMENT '绑定手机号' AFTER email,
ADD COLUMN role VARCHAR(10) NOT NULL DEFAULT 'USER' COMMENT '角色 USER/ADMIN' AFTER phone;

-- password 允许 NULL（普通用户无本地密码）
ALTER TABLE user MODIFY COLUMN password BLOB NULL COMMENT '应用密码(PBKDF2哈希，仅管理员用，可空)';

-- 迁移管理员角色（adminConfig.username 匹配的行置为 ADMIN，请按实际配置调整）
-- UPDATE user SET role = 'ADMIN' WHERE username = '<admin_username>';

DESC user;
