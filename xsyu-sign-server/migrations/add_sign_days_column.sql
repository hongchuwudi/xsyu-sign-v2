-- 为 user 表添加 sign_days 字段
-- 用于配置用户的签到日期
-- 0=周日，1=周一，2=周二，3=周三，4=周四，5=周五，6=周六

USE xsyu;

-- 添加 sign_days 字段，默认值为每天签到
ALTER TABLE user 
ADD COLUMN sign_days VARCHAR(50) DEFAULT '0,1,2,3,4,5,6' 
COMMENT '签到日期配置（0=周日，1=周一，...，6=周六）' 
AFTER auto_sign;

-- 更新现有用户的默认值
UPDATE user 
SET sign_days = '0,1,2,3,4,5,6' 
WHERE sign_days IS NULL;

-- 查看修改结果
DESC user;
