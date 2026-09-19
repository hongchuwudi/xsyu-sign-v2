-- 用户体系重构：学号即用户名（username = 学号，唯一标识）
-- phone: 绑定手机号（短信登录时记录）
-- role: USER / ADMIN
-- password: 统一密码字段。管理员存 PBKDF2 哈希；普通用户存 AES-GCM 学校密码；短信/扫码用户为 NULL

USE xsyu;

ALTER TABLE user
ADD COLUMN phone VARCHAR(20) NULL COMMENT '绑定手机号' AFTER email,
ADD COLUMN role VARCHAR(10) NOT NULL DEFAULT 'USER' COMMENT '角色 USER/ADMIN' AFTER phone;

-- password 允许 NULL（短信/扫码用户不保存学校密码）
ALTER TABLE user MODIFY COLUMN password BLOB NULL COMMENT '统一密码字段：管理员PBKDF2哈希/普通用户AES-GCM学校密码/可空';

-- 迁移管理员角色（adminConfig.username 匹配的行置为 ADMIN，请按实际配置调整）
-- UPDATE user SET role = 'ADMIN' WHERE username = '<admin_username>';

DESC user;
