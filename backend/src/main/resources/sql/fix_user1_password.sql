USE music_db;

-- 修复 user1 的密码（正确的 BCrypt 格式，密码为 123456）
UPDATE user SET password = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj3n3.zmdr9k7uOCQb376NoUu' WHERE username = 'user1';

-- 验证修复结果
SELECT username, role, LENGTH(password) as pass_len, SUBSTRING(password, 1, 10) as pass_start FROM user WHERE username = 'user1';
