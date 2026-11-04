USE music_db;

-- 删除旧的 admin 用户
DELETE FROM user WHERE username = 'admin';

-- 插入新的 admin 用户（使用 BCrypt 加密的密码 123456）
INSERT INTO user (username, password, nickname, role, status, create_time) 
VALUES ('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj3n3.zmdr9k7uOCQb376NoUu', '管理员', 'ADMIN', 1, NOW());

-- 验证
SELECT username, LENGTH(password) as pass_len, role, status FROM user WHERE username = 'admin';
