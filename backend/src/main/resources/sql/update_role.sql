-- 更新 user 表添加 role 字段
USE music_db;

-- 添加 role 字段（如果不存在）
ALTER TABLE `user` 
ADD COLUMN IF NOT EXISTS `role` VARCHAR(20) DEFAULT 'USER' COMMENT '角色：ADMIN-管理员 USER-普通用户';

-- 添加索引
ALTER TABLE `user` ADD INDEX IF NOT EXISTS idx_role (`role`);

-- 将 admin 用户设置为管理员角色
UPDATE `user` SET `role` = 'ADMIN' WHERE `username` = 'admin';

-- 其他用户设置为普通用户
UPDATE `user` SET `role` = 'USER' WHERE `username` != 'admin' AND `role` IS NULL;
