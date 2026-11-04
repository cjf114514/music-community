CREATE DATABASE IF NOT EXISTS music_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE music_db;

CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `username` VARCHAR(50) NOT NULL UNIQUE,
    `password` VARCHAR(255) NOT NULL,
    `email` VARCHAR(100),
    `avatar` VARCHAR(255),
    `nickname` VARCHAR(50),
    `gender` TINYINT DEFAULT 0 COMMENT '0-未知 1-男 2-女',
    `birthday` DATE,
    `signature` VARCHAR(200),
    `status` TINYINT DEFAULT 1 COMMENT '0-禁用 1-正常',
    `role` VARCHAR(20) DEFAULT 'USER' COMMENT '角色：ADMIN-管理员 USER-普通用户',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_role (`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `song` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(100) NOT NULL,
    `singer` VARCHAR(100),
    `album` VARCHAR(100),
    `url` VARCHAR(255),
    `cover_url` VARCHAR(255),
    `lyric` TEXT COMMENT 'LRC歌词',
    `duration` BIGINT COMMENT '时长(秒)',
    `file_size` BIGINT COMMENT '文件大小(字节)',
    `format` VARCHAR(10) DEFAULT 'mp3',
    `play_count` BIGINT DEFAULT 0 COMMENT '播放次数',
    `status` TINYINT DEFAULT 1 COMMENT '0-下架 1-正常',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_name (`name`),
    INDEX idx_singer (`singer`),
    INDEX idx_album (`album`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `playlist` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(100) NOT NULL,
    `cover_url` VARCHAR(255),
    `description` VARCHAR(500),
    `user_id` BIGINT NOT NULL,
    `is_public` TINYINT DEFAULT 1 COMMENT '0-私密 1-公开',
    `song_count` INT DEFAULT 0,
    `play_count` BIGINT DEFAULT 0,
    `status` TINYINT DEFAULT 1 COMMENT '0-删除 1-正常',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (`user_id`),
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `playlist_song` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `playlist_id` BIGINT NOT NULL,
    `song_id` BIGINT NOT NULL,
    `sort_order` INT DEFAULT 0,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_playlist_song (`playlist_id`, `song_id`),
    INDEX idx_playlist_id (`playlist_id`),
    INDEX idx_song_id (`song_id`),
    FOREIGN KEY (`playlist_id`) REFERENCES `playlist`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`song_id`) REFERENCES `song`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `comment` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `content` VARCHAR(500) NOT NULL,
    `user_id` BIGINT NOT NULL,
    `song_id` BIGINT,
    `playlist_id` BIGINT,
    `parent_id` BIGINT DEFAULT NULL COMMENT '父评论ID',
    `like_count` INT DEFAULT 0,
    `status` TINYINT DEFAULT 1 COMMENT '0-删除 1-正常',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_song_id (`song_id`),
    INDEX idx_playlist_id (`playlist_id`),
    INDEX idx_user_id (`user_id`),
    INDEX idx_parent_id (`parent_id`),
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`song_id`) REFERENCES `song`(`id`) ON DELETE SET NULL,
    FOREIGN KEY (`playlist_id`) REFERENCES `playlist`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `favorite` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `song_id` BIGINT NOT NULL,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_song (`user_id`, `song_id`),
    INDEX idx_user_id (`user_id`),
    INDEX idx_song_id (`song_id`),
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`song_id`) REFERENCES `song`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `play_history` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `song_id` BIGINT NOT NULL,
    `play_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (`user_id`),
    INDEX idx_song_id (`song_id`),
    INDEX idx_play_time (`play_time`),
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`song_id`) REFERENCES `song`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `user` (`username`, `password`, `nickname`, `email`, `avatar`, `gender`, `role`) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '管理员', 'admin@music.com', NULL, 0, 'ADMIN'),
('user1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '音乐达人', 'user1@music.com', NULL, 1, 'USER'),
('user2', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '听歌小能手', 'user2@music.com', NULL, 2, 'USER');

INSERT INTO `song` (`name`, `singer`, `album`, `url`, `cover_url`, `duration`, `play_count`) VALUES
('晴天', '周杰伦', '叶惠美', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3', 'https://picsum.photos/seed/song1/300/300', 269, 0),
('七里香', '周杰伦', '七里香', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3', 'https://picsum.photos/seed/song2/300/300', 299, 0),
('稻香', '周杰伦', '魔杰座', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3', 'https://picsum.photos/seed/song3/300/300', 223, 0),
('夜曲', '周杰伦', '十一月的萧邦', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3', 'https://picsum.photos/seed/song4/300/300', 226, 0),
('告白气球', '周杰伦', '周杰伦的床边故事', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3', 'https://picsum.photos/seed/song5/300/300', 215, 0),
('演员', '薛之谦', '绅士', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3', 'https://picsum.photos/seed/song6/300/300', 240, 0),
('丑八怪', '薛之谦', '意外', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-7.mp3', 'https://picsum.photos/seed/song7/300/300', 252, 0),
('平凡之路', '朴树', '猎户星座', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3', 'https://picsum.photos/seed/song8/300/300', 286, 0),
('那些年', '胡夏', '那些年', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-9.mp3', 'https://picsum.photos/seed/song9/300/300', 245, 0),
('小幸运', '田馥甄', '小幸运', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-10.mp3', 'https://picsum.photos/seed/song10/300/300', 275, 0),
('起风了', '买辣椒也用券', '起风了', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-11.mp3', 'https://picsum.photos/seed/song11/300/300', 325, 0),
('光年之外', '邓紫棋', '光年之外', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-12.mp3', 'https://picsum.photos/seed/song12/300/300', 232, 0),
('说散就散', '袁娅维', '说散就散', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-13.mp3', 'https://picsum.photos/seed/song13/300/300', 218, 0),
('体面', '于文文', '体面', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-14.mp3', 'https://picsum.photos/seed/song14/300/300', 265, 0),
('后来', '刘若英', '我等你', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-15.mp3', 'https://picsum.photos/seed/song15/300/300', 323, 0);

INSERT INTO `playlist` (`name`, `description`, `user_id`, `is_public`, `song_count`) VALUES
('华语经典', '精选华语流行经典歌曲', 1, 1, 5),
('伤感音乐', '适合一个人听的歌', 2, 1, 5),
('我的最爱', '个人收藏', 1, 1, 3);

INSERT INTO `playlist_song` (`playlist_id`, `song_id`, `sort_order`) VALUES
(1, 1, 1), (1, 2, 2), (1, 3, 3), (1, 4, 4), (1, 5, 5),
(2, 6, 1), (2, 7, 2), (2, 8, 3), (2, 9, 4), (2, 10, 5),
(3, 11, 1), (3, 12, 2), (3, 13, 3);

INSERT INTO `comment` (`content`, `user_id`, `song_id`, `like_count`) VALUES
('周杰伦的歌永远的神！', 2, 1, 10),
('这首歌听了无数遍', 1, 2, 5),
('前奏一响就泪目了', 2, 3, 8),
('经典永不过时', 1, 4, 3),
('每天必听', 2, 5, 12);

INSERT INTO `favorite` (`user_id`, `song_id`) VALUES
(1, 1), (1, 2), (1, 3), (2, 4), (2, 5), (1, 6);
