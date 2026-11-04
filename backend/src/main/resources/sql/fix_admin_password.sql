USE music_db;
UPDATE user SET password = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj3n3.zmdr9k7uOCQb376NoUu' WHERE username = 'admin';
SELECT LENGTH(password) as pass_len, username, password FROM user WHERE username = 'admin';
