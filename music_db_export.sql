-- ============================================================
--  music_db 演示数据（已脱敏）
--
--  导入步骤：
--    1) mysql -uroot -p -e "CREATE DATABASE music_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
--    2) mysql -uroot -p music_db < music_db_export.sql
--
--  演示账号（密码统一为 Demo@123456）：
--    cjf            管理员
--    cjf1/cjf2/cjf5 普通用户
--
--  脱敏说明：
--    · 所有邮箱替换为 <用户名>@example.com
--    · 所有密码哈希替换为同一演示哈希（明文 Demo@123456）
--    · 操作日志 IP 统一为 127.0.0.1
--  真实生产数据请另存为本地备份，不要提交进仓库。
-- ============================================================

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
DROP TABLE IF EXISTS `announcement`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `announcement` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `content` text,
  `create_time` datetime(6) DEFAULT NULL,
  `creator_id` bigint NOT NULL,
  `is_top` bit(1) NOT NULL,
  `status` varchar(20) NOT NULL,
  `title` varchar(200) NOT NULL,
  `type` varchar(20) DEFAULT NULL,
  `update_time` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `announcement` WRITE;
/*!40000 ALTER TABLE `announcement` DISABLE KEYS */;
INSERT INTO `announcement` VALUES (3,'测试内容','2026-05-12 20:19:17.995073',7,_binary '\0','published','test02','system','2026-05-17 00:48:03.832466');
/*!40000 ALTER TABLE `announcement` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `banner`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `banner` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `bg_color` varchar(100) DEFAULT NULL,
  `create_time` datetime(6) DEFAULT NULL,
  `description` varchar(200) DEFAULT NULL,
  `image_url` varchar(500) DEFAULT NULL,
  `link_id` bigint DEFAULT NULL,
  `link_type` varchar(20) DEFAULT NULL,
  `sort_order` int NOT NULL,
  `title` varchar(100) NOT NULL,
  `update_time` datetime(6) DEFAULT NULL,
  `visible` bit(1) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `banner` WRITE;
/*!40000 ALTER TABLE `banner` DISABLE KEYS */;
/*!40000 ALTER TABLE `banner` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `comment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `comment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `content` varchar(500) NOT NULL,
  `user_id` bigint NOT NULL,
  `song_id` bigint DEFAULT NULL,
  `playlist_id` bigint DEFAULT NULL,
  `parent_id` bigint DEFAULT NULL COMMENT '父评论ID',
  `like_count` int DEFAULT '0',
  `status` int DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `target_type` varchar(255) DEFAULT NULL,
  `audit_status` int DEFAULT NULL,
  `timestamp` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_song_id` (`song_id`),
  KEY `idx_playlist_id` (`playlist_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_parent_id` (`parent_id`),
  CONSTRAINT `comment_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `comment_ibfk_2` FOREIGN KEY (`song_id`) REFERENCES `song` (`id`) ON DELETE SET NULL,
  CONSTRAINT `comment_ibfk_3` FOREIGN KEY (`playlist_id`) REFERENCES `playlist` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `comment` WRITE;
/*!40000 ALTER TABLE `comment` DISABLE KEYS */;
INSERT INTO `comment` VALUES (7,'牛逼在哪',7,7,NULL,6,0,1,'2026-04-21 22:16:04','2026-05-17 00:47:56',NULL,NULL,NULL),(8,'牛逼',7,7,NULL,6,1,1,'2026-04-21 22:16:16','2026-04-21 22:16:18',NULL,NULL,NULL),(11,'1',7,NULL,7,NULL,0,1,'2026-05-17 23:35:21','2026-05-17 23:35:21','playlist',NULL,NULL),(13,'123',7,NULL,10,NULL,0,1,'2026-06-04 10:17:58','2026-06-04 10:17:58','playlist',NULL,NULL),(14,'12',7,NULL,10,NULL,0,1,'2026-06-04 10:18:00','2026-06-04 10:18:00','playlist',NULL,NULL),(15,'NIHAO',7,NULL,10,13,0,0,'2026-06-04 10:18:07','2026-06-04 10:20:05','playlist',NULL,NULL);
/*!40000 ALTER TABLE `comment` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `comment_like`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `comment_like` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `comment_id` bigint NOT NULL,
  `create_time` datetime(6) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKo8urnlomri10dub10y8ket23c` (`comment_id`,`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `comment_like` WRITE;
/*!40000 ALTER TABLE `comment_like` DISABLE KEYS */;
/*!40000 ALTER TABLE `comment_like` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `community_post`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `community_post` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `comment_count` int NOT NULL,
  `content` text NOT NULL,
  `cover_url` varchar(255) DEFAULT NULL,
  `create_time` datetime(6) DEFAULT NULL,
  `like_count` int NOT NULL,
  `title` varchar(255) NOT NULL,
  `update_time` datetime(6) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `image_urls` text,
  `related_playlist_id` bigint DEFAULT NULL,
  `related_song_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `community_post` WRITE;
/*!40000 ALTER TABLE `community_post` DISABLE KEYS */;
INSERT INTO `community_post` VALUES (1,1,'123',NULL,'2026-04-26 01:10:51.607540',6,'音乐分享','2026-04-26 16:41:27.315525',7,NULL,NULL,NULL),(2,0,'2','','2026-04-26 16:41:37.058934',0,'123','2026-04-26 16:41:37.058934',8,NULL,NULL,NULL),(3,0,'test','http://localhost:8080/uploads/post-image/b6a4ec65-00e3-4947-b9ba-a0148488165c.png','2026-04-26 16:41:57.032452',1,'123','2026-04-26 16:50:45.452052',8,NULL,NULL,NULL),(4,4,'4','http://localhost:8080/uploads/post-image/04f93a1d-3f8c-4921-ae82-5594bc639b53.webp','2026-04-26 16:52:53.523731',1,'123','2026-05-10 16:24:24.472739',8,NULL,NULL,NULL),(7,2,'test','/uploads/post-image/16fcab1e-d6cd-41dd-96e0-3f0eb29ae7bc.webp','2026-05-08 14:12:09.174679',0,'cjf5','2026-05-17 01:19:11.611962',17,NULL,NULL,NULL),(8,4,'test','/uploads/post-image/1c00a268-f21a-4ece-83f9-bbce0d3978fa.png','2026-05-10 16:23:40.281755',0,'1','2026-05-13 00:22:39.461855',7,NULL,NULL,NULL),(9,1,'test2','/uploads/post-image/4af5061c-b8c6-4e9a-97aa-7d87278f02f4.png','2026-05-10 22:52:48.550134',0,'2','2026-05-17 00:48:32.320455',7,'[\"/uploads/post-image/4af5061c-b8c6-4e9a-97aa-7d87278f02f4.png\"]',NULL,2),(10,0,'歌单无法评论 歌曲无法评论','','2026-05-17 00:49:39.936246',0,'当前问题','2026-05-17 00:49:39.936246',7,'',NULL,NULL),(11,1,'测试1','/uploads/post-image/052113aa-187a-4426-a605-bc62db08b329.webp','2026-05-18 14:51:21.030865',1,'测试','2026-05-22 14:21:23.099303',7,'[\"/uploads/post-image/052113aa-187a-4426-a605-bc62db08b329.webp\"]',NULL,2),(13,0,'测试内容','/uploads/post-image/077ba7a4-aa77-42ff-918e-c8ceacab0091.png','2026-06-04 10:13:50.532328',0,'测试','2026-06-04 10:13:50.532328',7,'[\"/uploads/post-image/077ba7a4-aa77-42ff-918e-c8ceacab0091.png\"]',NULL,2);
/*!40000 ALTER TABLE `community_post` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `conversation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `conversation` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `create_time` datetime(6) DEFAULT NULL,
  `last_message_content` varchar(500) DEFAULT NULL,
  `last_message_id` bigint DEFAULT NULL,
  `last_message_time` datetime(6) DEFAULT NULL,
  `update_time` datetime(6) DEFAULT NULL,
  `user1_id` bigint NOT NULL,
  `user1_unread` int NOT NULL,
  `user2_id` bigint NOT NULL,
  `user2_unread` int NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `conversation` WRITE;
/*!40000 ALTER TABLE `conversation` DISABLE KEYS */;
INSERT INTO `conversation` VALUES (1,'2026-05-03 22:17:04.337273','123',32,'2026-05-05 17:14:49.214252','2026-05-05 21:18:28.348486',7,0,8,0),(2,'2026-05-03 22:18:35.494900','123',26,'2026-05-03 23:24:12.652000','2026-05-03 23:24:12.654009',7,0,9,10),(3,'2026-05-04 22:14:03.976863',NULL,NULL,NULL,'2026-05-04 22:14:03.976863',7,0,10,0);
/*!40000 ALTER TABLE `conversation` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `favorite`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `favorite` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `song_id` bigint NOT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_song` (`user_id`,`song_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_song_id` (`song_id`),
  CONSTRAINT `favorite_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `favorite_ibfk_2` FOREIGN KEY (`song_id`) REFERENCES `song` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=20 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `favorite` WRITE;
/*!40000 ALTER TABLE `favorite` DISABLE KEYS */;
INSERT INTO `favorite` VALUES (8,8,1,'2026-04-26 22:41:47'),(14,7,1,'2026-05-03 23:34:27'),(18,7,4,'2026-06-04 10:12:53'),(19,7,5,'2026-06-04 10:13:08');
/*!40000 ALTER TABLE `favorite` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `notification`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notification` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `actor_id` bigint NOT NULL,
  `content` varchar(500) DEFAULT NULL,
  `create_time` datetime(6) DEFAULT NULL,
  `is_read` bit(1) NOT NULL,
  `target_id` bigint DEFAULT NULL,
  `type` varchar(30) NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `notification` WRITE;
/*!40000 ALTER TABLE `notification` DISABLE KEYS */;
INSERT INTO `notification` VALUES (1,7,'cjf 关注了你','2026-05-03 22:17:01.850146',_binary '',7,'FOLLOW',8),(2,7,'cjf 关注了你','2026-05-03 22:17:25.632602',_binary '',7,'FOLLOW',8),(3,9,' 关注了你','2026-05-03 22:18:34.878133',_binary '',9,'FOLLOW',7),(4,7,'cjf 关注了你','2026-05-03 23:03:12.992256',_binary '\0',7,'FOLLOW',9),(5,7,'cjf 评论了你的帖子','2026-05-10 16:24:07.995001',_binary '\0',4,'COMMENT_POST',8),(6,7,'cjf 评论了你的帖子','2026-05-10 16:24:24.470710',_binary '\0',4,'COMMENT_POST',8),(7,7,'系统公告：test1','2026-05-12 19:52:01.263012',_binary '\0',1,'announcement',8),(8,7,'系统公告：test1','2026-05-12 19:52:01.266655',_binary '\0',1,'announcement',9),(9,7,'系统公告：test1','2026-05-12 19:52:01.270955',_binary '',1,'announcement',17),(10,7,'系统公告：test02','2026-05-12 19:58:00.885015',_binary '\0',2,'announcement',8),(11,7,'系统公告：test02','2026-05-12 19:58:00.889057',_binary '\0',2,'announcement',9),(12,7,'系统公告：test02','2026-05-12 19:58:00.892705',_binary '',2,'announcement',17),(13,7,'系统公告：test01','2026-05-12 20:19:18.014356',_binary '\0',3,'announcement',8),(14,7,'系统公告：test01','2026-05-12 20:19:18.018472',_binary '\0',3,'announcement',9),(15,7,'系统公告：test01','2026-05-12 20:19:18.022040',_binary '',3,'announcement',17),(16,7,'cjf 评论了你的帖子','2026-05-17 01:19:05.559857',_binary '\0',7,'COMMENT_POST',17),(17,7,'cjf 评论了你的帖子','2026-05-17 01:19:11.609918',_binary '\0',7,'COMMENT_POST',17);
/*!40000 ALTER TABLE `notification` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `operation_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `operation_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `action` varchar(10) NOT NULL,
  `create_time` datetime(6) DEFAULT NULL,
  `description` varchar(200) DEFAULT NULL,
  `ip` varchar(45) DEFAULT NULL,
  `module` varchar(50) DEFAULT NULL,
  `target_id` bigint DEFAULT NULL,
  `target_type` varchar(50) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `username` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=28 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `operation_log` WRITE;
/*!40000 ALTER TABLE `operation_log` DISABLE KEYS */;
INSERT INTO `operation_log` VALUES (1,'CREATE','2026-05-12 19:52:01.290210','发布公告','127.0.0.1','公告管理',NULL,'announcement',7,'cjf'),(2,'UPDATE','2026-05-12 19:52:16.859729','切换公告状态','127.0.0.1','公告管理',1,'announcement',7,'cjf'),(3,'UPDATE','2026-05-12 19:52:18.483556','切换公告状态','127.0.0.1','公告管理',1,'announcement',7,'cjf'),(4,'CREATE','2026-05-12 19:58:00.905365','发布公告','127.0.0.1','公告管理',NULL,'announcement',7,'cjf'),(5,'UPDATE','2026-05-12 19:59:51.482085','编辑歌曲','127.0.0.1','歌曲管理',1,'song',7,'cjf'),(6,'DELETE','2026-05-12 20:19:08.296763','删除公告','127.0.0.1','公告管理',1,'announcement',7,'cjf'),(7,'DELETE','2026-05-12 20:19:10.367979','删除公告','127.0.0.1','公告管理',2,'announcement',7,'cjf'),(8,'CREATE','2026-05-12 20:19:18.036573','发布公告','127.0.0.1','公告管理',NULL,'announcement',7,'cjf'),(9,'DELETE','2026-05-12 23:23:13.625538','删除评论','127.0.0.1','评论管理',6,'comment',7,'cjf'),(10,'DELETE','2026-05-12 23:46:00.518246','删除社区评论','127.0.0.1','社区管理',7,'comment',7,'cjf'),(11,'DELETE','2026-05-12 23:49:33.191442','删除社区评论','127.0.0.1','社区管理',2,'comment',7,'cjf'),(12,'DELETE','2026-05-13 00:14:15.241393','删除社区评论','127.0.0.1','社区管理',8,'comment',7,'cjf'),(13,'UPDATE','2026-05-17 00:47:51.338209','修改歌单状态','127.0.0.1','歌单管理',7,'playlist',7,'cjf'),(14,'UPDATE','2026-05-17 00:47:52.162981','修改歌单状态','127.0.0.1','歌单管理',7,'playlist',7,'cjf'),(15,'UPDATE','2026-05-17 00:47:56.187691','修改评论状态','127.0.0.1','评论管理',7,'comment',7,'cjf'),(16,'UPDATE','2026-05-17 00:48:03.849847','编辑公告','127.0.0.1','公告管理',3,'announcement',7,'cjf'),(17,'UPDATE','2026-05-17 00:48:11.536794','修改用户状态','127.0.0.1','用户管理',8,'user',7,'cjf'),(18,'UPDATE','2026-05-17 00:48:13.267957','修改用户状态','127.0.0.1','用户管理',8,'user',7,'cjf'),(19,'CREATE','2026-05-18 15:00:00.798747','新增歌曲','127.0.0.1','歌曲管理',NULL,'song',7,'cjf'),(20,'UPDATE','2026-05-18 15:00:40.631741','编辑歌曲','127.0.0.1','歌曲管理',21,'song',7,'cjf'),(21,'DELETE','2026-06-04 09:15:00.731240','删除用户','127.0.0.1','用户管理',17,'user',7,'cjf'),(22,'CREATE','2026-06-04 10:08:47.251925','新增歌曲','127.0.0.1','歌曲管理',NULL,'song',7,'cjf'),(23,'DELETE','2026-06-04 10:13:27.162370','删除帖子','127.0.0.1','社区管理',12,'post',7,'cjf'),(24,'UPDATE','2026-06-04 10:16:29.336166','编辑歌曲','127.0.0.1','歌曲管理',2,'song',7,'cjf'),(25,'UPDATE','2026-06-04 10:20:05.478741','修改评论状态','127.0.0.1','评论管理',15,'comment',7,'cjf'),(26,'CREATE','2026-06-04 10:21:55.391729','新增歌曲','127.0.0.1','歌曲管理',NULL,'song',7,'cjf'),(27,'CREATE','2026-06-04 10:36:24.284158','新增歌曲','127.0.0.1','歌曲管理',NULL,'song',7,'cjf');
/*!40000 ALTER TABLE `operation_log` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `play_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `play_history` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `song_id` bigint NOT NULL,
  `play_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_song_id` (`song_id`),
  KEY `idx_play_time` (`play_time`),
  CONSTRAINT `play_history_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `play_history_ibfk_2` FOREIGN KEY (`song_id`) REFERENCES `song` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=174 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `play_history` WRITE;
/*!40000 ALTER TABLE `play_history` DISABLE KEYS */;
INSERT INTO `play_history` VALUES (13,7,1,'2026-04-24 15:08:43'),(16,7,7,'2026-04-24 15:46:22'),(19,7,1,'2026-04-24 16:27:39'),(20,7,9,'2026-04-24 16:27:52'),(21,7,1,'2026-04-24 16:27:57'),(22,7,1,'2026-04-24 16:36:01'),(25,7,4,'2026-04-24 16:51:00'),(26,7,5,'2026-04-24 16:56:05'),(32,8,1,'2026-04-26 22:26:22'),(36,8,1,'2026-04-26 22:27:24'),(38,8,1,'2026-04-26 22:31:40'),(40,8,1,'2026-04-26 22:32:13'),(43,7,1,'2026-04-26 23:43:13'),(45,8,1,'2026-04-27 14:10:54'),(46,8,1,'2026-04-27 14:45:07'),(47,8,1,'2026-04-27 14:45:19'),(48,8,1,'2026-04-27 14:45:25'),(52,8,4,'2026-04-27 14:45:37'),(59,8,1,'2026-04-29 10:15:37'),(64,8,13,'2026-04-29 10:31:53'),(71,8,4,'2026-05-03 23:34:04'),(86,7,1,'2026-05-04 21:55:17'),(94,7,4,'2026-05-04 22:32:41'),(105,7,1,'2026-05-05 01:05:35'),(106,7,1,'2026-05-05 01:16:07'),(108,7,1,'2026-05-05 01:16:18'),(111,7,1,'2026-05-05 01:16:51'),(151,7,8,'2026-05-10 16:32:05'),(164,7,1,'2026-05-22 14:22:46'),(166,7,1,'2026-05-22 14:36:45'),(167,7,4,'2026-06-03 17:49:11'),(169,7,6,'2026-06-03 23:45:14');
/*!40000 ALTER TABLE `play_history` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `playlist`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `playlist` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL,
  `cover_url` varchar(255) DEFAULT NULL,
  `description` varchar(500) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `is_public` int DEFAULT NULL,
  `song_count` int DEFAULT '0',
  `play_count` bigint DEFAULT '0',
  `status` int DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `category` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  CONSTRAINT `playlist_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `playlist` WRITE;
/*!40000 ALTER TABLE `playlist` DISABLE KEYS */;
INSERT INTO `playlist` VALUES (7,'test01','http://localhost:8080/uploads/cover/9a0556ce-e0d0-4927-ba4d-c2b860ab426f.webp','测试歌单',8,1,1,0,1,'2026-04-26 22:25:52','2026-05-17 00:47:52',NULL),(9,'周杰伦',NULL,'',7,1,2,0,1,'2026-05-05 00:58:54','2026-05-20 00:45:57',NULL),(10,'测试','/uploads/cover/a7424cfd-ae46-4884-91c8-c3b263d502c4.webp','',7,1,1,0,1,'2026-06-04 00:03:21','2026-06-04 10:17:51',NULL);
/*!40000 ALTER TABLE `playlist` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `playlist_share`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `playlist_share` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `create_time` datetime(6) DEFAULT NULL,
  `playlist_id` bigint NOT NULL,
  `share_count` int NOT NULL,
  `share_token` varchar(100) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_5cud0pvgl0p9ioi7eq5wcunp9` (`share_token`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `playlist_share` WRITE;
/*!40000 ALTER TABLE `playlist_share` DISABLE KEYS */;
INSERT INTO `playlist_share` VALUES (1,'2026-05-05 21:16:05.275589',7,1,'f8cfaa51',8);
/*!40000 ALTER TABLE `playlist_share` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `playlist_song`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `playlist_song` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `playlist_id` bigint NOT NULL,
  `song_id` bigint NOT NULL,
  `sort_order` int DEFAULT '0',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_playlist_song` (`playlist_id`,`song_id`),
  KEY `idx_playlist_id` (`playlist_id`),
  KEY `idx_song_id` (`song_id`),
  CONSTRAINT `playlist_song_ibfk_1` FOREIGN KEY (`playlist_id`) REFERENCES `playlist` (`id`) ON DELETE CASCADE,
  CONSTRAINT `playlist_song_ibfk_2` FOREIGN KEY (`song_id`) REFERENCES `song` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=20 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `playlist_song` WRITE;
/*!40000 ALTER TABLE `playlist_song` DISABLE KEYS */;
INSERT INTO `playlist_song` VALUES (18,9,4,1,'2026-05-20 00:45:57'),(19,10,10,0,'2026-06-04 10:17:51');
/*!40000 ALTER TABLE `playlist_song` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `post_comment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `post_comment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `content` text NOT NULL,
  `create_time` datetime(6) DEFAULT NULL,
  `like_count` int NOT NULL,
  `parent_id` bigint DEFAULT NULL,
  `post_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `post_comment` WRITE;
/*!40000 ALTER TABLE `post_comment` DISABLE KEYS */;
INSERT INTO `post_comment` VALUES (1,'4','2026-04-26 16:29:57.155502',0,NULL,1,8),(3,'123','2026-04-26 18:44:58.939325',0,NULL,4,8),(4,'1','2026-05-10 16:24:07.976182',0,3,4,7),(5,'@cjf 123','2026-05-10 16:24:24.462068',0,3,4,7),(6,'http://localhost:5173/community/post/4','2026-05-10 16:28:53.094558',1,NULL,8,7),(9,'123','2026-05-13 00:22:39.408475',0,NULL,8,7),(10,'123','2026-05-17 00:48:32.313652',0,NULL,9,7),(11,'12','2026-05-17 01:19:05.549976',0,NULL,7,7),(12,'1','2026-05-17 01:19:11.604004',0,11,7,7),(13,'1','2026-05-22 14:21:13.490294',0,NULL,11,7);
/*!40000 ALTER TABLE `post_comment` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `post_comment_like`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `post_comment_like` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `comment_id` bigint NOT NULL,
  `create_time` datetime(6) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKdoo0h59qijtf8h9o8ctrfrhc9` (`comment_id`,`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `post_comment_like` WRITE;
/*!40000 ALTER TABLE `post_comment_like` DISABLE KEYS */;
INSERT INTO `post_comment_like` VALUES (6,2,'2026-04-29 10:59:10.135976',8),(8,6,'2026-05-12 23:23:30.163428',7);
/*!40000 ALTER TABLE `post_comment_like` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `post_like`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `post_like` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `create_time` datetime(6) DEFAULT NULL,
  `post_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKpmmko3h7yonaqhy5gxvnmdeue` (`user_id`,`post_id`)
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `post_like` WRITE;
/*!40000 ALTER TABLE `post_like` DISABLE KEYS */;
INSERT INTO `post_like` VALUES (5,'2026-04-26 16:41:27.311408',1,8),(6,'2026-04-26 16:50:45.442781',3,8),(16,'2026-04-26 22:05:10.594636',4,8),(18,'2026-05-22 14:21:23.095382',11,7);
/*!40000 ALTER TABLE `post_like` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `post_report`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `post_report` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `create_time` datetime(6) DEFAULT NULL,
  `handle_remark` varchar(500) DEFAULT NULL,
  `handle_time` datetime(6) DEFAULT NULL,
  `handler_id` bigint DEFAULT NULL,
  `reason` varchar(500) DEFAULT NULL,
  `reporter_id` bigint NOT NULL,
  `status` varchar(20) NOT NULL,
  `target_id` bigint NOT NULL,
  `target_type` varchar(20) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `post_report` WRITE;
/*!40000 ALTER TABLE `post_report` DISABLE KEYS */;
INSERT INTO `post_report` VALUES (1,'2026-05-10 22:53:23.002500',NULL,NULL,NULL,'垃圾广告',7,'pending',7,'post'),(2,'2026-05-10 23:21:31.775426',NULL,NULL,NULL,'垃圾广告',7,'pending',2,'post'),(3,'2026-05-11 00:01:52.128961',NULL,NULL,NULL,'垃圾广告',7,'pending',4,'post');
/*!40000 ALTER TABLE `post_report` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `private_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `private_message` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `content` text NOT NULL,
  `conversation_id` bigint NOT NULL,
  `create_time` datetime(6) DEFAULT NULL,
  `is_read` bit(1) NOT NULL,
  `is_recalled` bit(1) NOT NULL,
  `message_type` varchar(20) NOT NULL,
  `receiver_id` bigint NOT NULL,
  `sender_id` bigint NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=33 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `private_message` WRITE;
/*!40000 ALTER TABLE `private_message` DISABLE KEYS */;
INSERT INTO `private_message` VALUES (1,'原神牛逼',1,'2026-05-03 22:17:15.106723',_binary '',_binary '\0','TEXT',8,7),(2,'原神牛逼',1,'2026-05-03 22:17:20.603701',_binary '',_binary '\0','TEXT',8,7),(3,'原神牛逼',1,'2026-05-03 22:17:29.600265',_binary '',_binary '\0','TEXT',8,7),(4,'test',2,'2026-05-03 22:18:39.029689',_binary '',_binary '\0','TEXT',7,9),(5,'1',2,'2026-05-03 22:18:53.008866',_binary '',_binary '\0','TEXT',7,9),(6,'123',2,'2026-05-03 22:19:14.068212',_binary '',_binary '\0','TEXT',7,9),(7,'123',1,'2026-05-03 22:20:15.494987',_binary '',_binary '\0','TEXT',7,8),(8,'123',1,'2026-05-03 22:21:21.377049',_binary '',_binary '\0','TEXT',7,8),(9,'123',1,'2026-05-03 22:21:32.780550',_binary '',_binary '\0','TEXT',7,8),(10,'123',1,'2026-05-03 22:41:54.858517',_binary '',_binary '\0','TEXT',7,8),(11,'test',1,'2026-05-03 22:42:01.042038',_binary '',_binary '\0','TEXT',7,8),(12,'test',1,'2026-05-03 22:42:09.533126',_binary '',_binary '\0','TEXT',7,8),(13,'123',2,'2026-05-03 22:43:01.251653',_binary '\0',_binary '\0','TEXT',9,7),(14,'123',1,'2026-05-03 22:44:01.345154',_binary '',_binary '\0','TEXT',8,7),(15,'123',1,'2026-05-03 23:02:38.528591',_binary '',_binary '\0','TEXT',8,7),(16,'123',1,'2026-05-03 23:02:56.844100',_binary '',_binary '\0','TEXT',8,7),(17,'123',2,'2026-05-03 23:03:15.341514',_binary '\0',_binary '\0','TEXT',9,7),(18,'test',2,'2026-05-03 23:04:09.274979',_binary '\0',_binary '\0','TEXT',9,7),(19,'123',2,'2026-05-03 23:05:54.168640',_binary '\0',_binary '\0','TEXT',9,7),(20,'123',2,'2026-05-03 23:07:22.789342',_binary '\0',_binary '\0','TEXT',9,7),(21,'123',2,'2026-05-03 23:07:55.044619',_binary '\0',_binary '\0','TEXT',9,7),(22,'test',2,'2026-05-03 23:17:15.531770',_binary '\0',_binary '\0','TEXT',9,7),(23,'123',1,'2026-05-03 23:17:42.925168',_binary '',_binary '\0','TEXT',8,7),(24,'213',2,'2026-05-03 23:17:47.168195',_binary '\0',_binary '\0','TEXT',9,7),(25,'123',2,'2026-05-03 23:18:04.611066',_binary '\0',_binary '\0','TEXT',9,7),(26,'123',2,'2026-05-03 23:24:12.652000',_binary '\0',_binary '\0','TEXT',9,7),(27,'123',1,'2026-05-03 23:24:32.661315',_binary '',_binary '\0','TEXT',8,7),(28,'sssssssssssssssssssssssssssssssssssss',1,'2026-05-03 23:24:41.760958',_binary '',_binary '\0','TEXT',8,7),(29,'123',1,'2026-05-03 23:33:27.152823',_binary '',_binary '\0','TEXT',7,8),(30,'123',1,'2026-05-03 23:35:48.232652',_binary '',_binary '\0','TEXT',8,7),(31,'123',1,'2026-05-04 16:59:03.835401',_binary '',_binary '\0','TEXT',8,7),(32,'123',1,'2026-05-05 17:14:49.214252',_binary '',_binary '\0','TEXT',7,8);
/*!40000 ALTER TABLE `private_message` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `search_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `search_history` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `keyword` varchar(100) NOT NULL,
  `search_time` datetime(6) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=60 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `search_history` WRITE;
/*!40000 ALTER TABLE `search_history` DISABLE KEYS */;
INSERT INTO `search_history` VALUES (1,'七','2026-04-24 16:06:09.303181',7),(2,'七','2026-04-24 16:06:09.388187',7),(3,'周','2026-04-24 16:06:12.390201',7),(4,'周','2026-04-24 16:06:21.351964',7),(5,'叶','2026-04-24 16:06:28.734414',7),(6,'1','2026-04-24 16:06:38.599363',7),(7,'1','2026-04-24 16:19:59.673173',7),(8,'1','2026-04-24 16:27:08.926822',7),(9,'1','2026-04-24 16:45:57.376517',7),(10,'1','2026-04-24 16:46:03.836057',7),(11,'周','2026-04-24 16:46:10.149199',7),(12,'周','2026-04-24 16:46:12.119891',7),(13,'周','2026-04-24 16:46:14.388907',7),(14,'周','2026-04-24 16:46:14.445720',7),(15,'3','2026-04-24 16:46:24.425172',7),(16,'叶','2026-04-24 16:46:31.634472',7),(17,'叶','2026-04-24 16:46:31.685200',7),(18,'周','2026-04-24 16:49:15.345523',7),(19,'周','2026-04-24 16:49:17.254397',7),(20,'周','2026-04-24 16:49:23.374492',7),(21,'1','2026-04-24 16:49:28.097784',7),(22,'1','2026-04-24 16:49:45.819921',7),(23,'周','2026-04-24 16:49:50.818039',7),(24,'晴','2026-04-24 16:49:57.868594',7),(25,'晴','2026-04-24 16:50:02.351421',7),(26,'周','2026-04-24 19:50:44.771268',7),(27,'周','2026-04-26 23:00:26.283540',8),(28,'周','2026-04-26 23:00:28.366385',8),(29,'周','2026-04-26 23:00:33.293114',8),(30,'周','2026-04-26 23:00:33.351894',8),(31,'周','2026-04-26 23:03:29.990615',8),(32,'周','2026-04-26 23:03:30.100837',8),(33,'周','2026-04-26 23:07:40.489031',8),(34,'周','2026-04-26 23:07:40.619361',8),(35,'周','2026-04-26 23:07:52.416007',8),(36,'林','2026-04-26 23:07:58.274768',8),(37,'周','2026-04-26 23:08:03.368392',8),(38,'1','2026-04-26 23:08:07.484049',8),(39,'周','2026-04-27 16:24:03.240138',12),(40,'周','2026-04-27 16:24:03.482721',12),(41,'周','2026-05-04 22:13:50.226904',7),(42,'周','2026-05-04 22:13:50.472590',7),(43,'cjf','2026-05-04 22:14:00.223971',7),(44,'cjf','2026-05-04 22:14:00.290251',7),(45,'周','2026-05-05 21:15:53.758118',8),(46,'周','2026-05-05 21:15:53.957299',8),(47,'周','2026-05-06 23:21:23.469903',7),(48,'周','2026-05-06 23:21:23.820417',7),(49,'周','2026-05-15 13:49:50.703190',7),(50,'周','2026-05-15 13:49:50.978442',7),(51,'cjf','2026-05-15 13:49:57.516319',7),(52,'cjf','2026-05-15 13:49:59.511227',7),(53,'周','2026-05-15 13:50:00.857010',7),(54,'cjf','2026-05-15 13:50:05.388618',7),(55,'cjf','2026-05-15 13:50:05.455156',7),(56,'cj','2026-05-15 13:50:11.042636',7),(57,'cjf2','2026-05-15 13:50:17.728803',7),(58,'周','2026-05-22 14:28:11.666726',7),(59,'周','2026-05-22 14:28:11.815492',7);
/*!40000 ALTER TABLE `search_history` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `singer`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `singer` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `avatar` varchar(255) DEFAULT NULL,
  `create_time` datetime(6) DEFAULT NULL,
  `description` text,
  `name` varchar(100) NOT NULL,
  `song_count` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_plxkng25bala4so3hb8nwbdi6` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `singer` WRITE;
/*!40000 ALTER TABLE `singer` DISABLE KEYS */;
/*!40000 ALTER TABLE `singer` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `song`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `song` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL,
  `singer` varchar(100) DEFAULT NULL,
  `album` varchar(100) DEFAULT NULL,
  `url` varchar(255) DEFAULT NULL,
  `cover_url` varchar(255) DEFAULT NULL,
  `lyric` text COMMENT 'LRC歌词',
  `duration` bigint DEFAULT NULL COMMENT '时长(秒)',
  `file_size` bigint DEFAULT NULL COMMENT '文件大小(字节)',
  `format` varchar(10) DEFAULT 'mp3',
  `play_count` bigint DEFAULT '0' COMMENT '播放次数',
  `status` int DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `genre` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_name` (`name`),
  KEY `idx_singer` (`singer`),
  KEY `idx_album` (`album`)
) ENGINE=InnoDB AUTO_INCREMENT=25 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `song` WRITE;
/*!40000 ALTER TABLE `song` DISABLE KEYS */;
INSERT INTO `song` VALUES (1,'晴天1','周杰伦','叶惠美','https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3','https://picsum.photos/seed/song1/300/300',NULL,269,NULL,'mp3',0,1,'2026-04-21 14:47:44','2026-05-12 19:59:51',NULL),(4,'夜曲','周杰伦','十一月的萧邦','https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3','https://picsum.photos/seed/song4/300/300',NULL,226,NULL,'mp3',0,1,'2026-04-21 14:47:44','2026-04-21 20:21:33',NULL),(5,'告白气球','周杰伦','周杰伦的床边故事','https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3','https://picsum.photos/seed/song5/300/300',NULL,215,NULL,'mp3',0,1,'2026-04-21 14:47:44','2026-04-21 20:21:33',NULL),(6,'演员','薛之谦','绅士','https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3','https://picsum.photos/seed/song6/300/300',NULL,240,NULL,'mp3',0,1,'2026-04-21 14:47:44','2026-04-21 20:21:33',NULL),(7,'丑八怪','薛之谦','意外','https://www.soundhelix.com/examples/mp3/SoundHelix-Song-7.mp3','https://picsum.photos/seed/song7/300/300',NULL,252,NULL,'mp3',3,1,'2026-04-21 14:47:44','2026-04-21 22:16:16',NULL),(8,'平凡之路','朴树','猎户星座','https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3','https://picsum.photos/seed/song8/300/300',NULL,286,NULL,'mp3',0,1,'2026-04-21 14:47:44','2026-04-21 20:21:33',NULL),(9,'那些年','胡夏','那些年','https://www.soundhelix.com/examples/mp3/SoundHelix-Song-9.mp3','https://picsum.photos/seed/song9/300/300',NULL,245,NULL,'mp3',0,1,'2026-04-21 14:47:44','2026-04-21 20:21:33',NULL),(10,'小幸运','田馥甄','小幸运','https://www.soundhelix.com/examples/mp3/SoundHelix-Song-10.mp3','https://picsum.photos/seed/song10/300/300',NULL,275,NULL,'mp3',0,1,'2026-04-21 14:47:44','2026-04-21 20:21:33',NULL),(11,'起风了','买辣椒也用券','起风了','https://www.soundhelix.com/examples/mp3/SoundHelix-Song-11.mp3','https://picsum.photos/seed/song11/300/300',NULL,325,NULL,'mp3',0,1,'2026-04-21 14:47:44','2026-04-21 20:21:33',NULL),(12,'光年之外','邓紫棋','光年之外','https://www.soundhelix.com/examples/mp3/SoundHelix-Song-12.mp3','https://picsum.photos/seed/song12/300/300',NULL,232,NULL,'mp3',0,1,'2026-04-21 14:47:44','2026-04-21 20:21:33',NULL),(13,'说散就散','袁娅维','说散就散','https://www.soundhelix.com/examples/mp3/SoundHelix-Song-13.mp3','https://picsum.photos/seed/song13/300/300',NULL,218,NULL,'mp3',0,1,'2026-04-21 14:47:44','2026-04-21 20:21:33',NULL),(14,'体面','于文文','体面','https://www.soundhelix.com/examples/mp3/SoundHelix-Song-14.mp3','https://picsum.photos/seed/song14/300/300',NULL,265,NULL,'mp3',0,1,'2026-04-21 14:47:44','2026-04-21 20:21:33',NULL),(15,'后来','刘若英','我等你','https://www.soundhelix.com/examples/mp3/SoundHelix-Song-15.mp3','https://picsum.photos/seed/song15/300/300',NULL,323,NULL,'mp3',0,1,'2026-04-21 14:47:44','2026-04-21 20:21:33',NULL);
/*!40000 ALTER TABLE `song` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `song_share`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `song_share` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `create_time` datetime(6) DEFAULT NULL,
  `share_count` int NOT NULL,
  `share_token` varchar(100) DEFAULT NULL,
  `song_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_qjdj2feydvgtws5w8ss2yn5l` (`share_token`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `song_share` WRITE;
/*!40000 ALTER TABLE `song_share` DISABLE KEYS */;
INSERT INTO `song_share` VALUES (1,'2026-04-27 15:10:39.048905',2,'cc8d6483',1,7);
/*!40000 ALTER TABLE `song_share` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `system_setting`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `system_setting` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `description` varchar(200) DEFAULT NULL,
  `key` varchar(100) NOT NULL,
  `value` text,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_15goxnw5rcgxagnl4de5eopkt` (`key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `system_setting` WRITE;
/*!40000 ALTER TABLE `system_setting` DISABLE KEYS */;
/*!40000 ALTER TABLE `system_setting` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL,
  `password` varchar(255) NOT NULL,
  `email` varchar(100) DEFAULT NULL,
  `avatar` varchar(255) DEFAULT NULL,
  `nickname` varchar(50) DEFAULT NULL,
  `gender` int DEFAULT NULL,
  `birthday` date DEFAULT NULL,
  `signature` varchar(200) DEFAULT NULL,
  `status` int DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `role` varchar(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `user` WRITE;
/*!40000 ALTER TABLE `user` DISABLE KEYS */;
INSERT INTO `user` VALUES (7,'cjf','$2a$10$ANMMW0tvJcPA0XhS4LAx4.XHUgP.FZAn57tVxIMLCz3x4CuGktQky','cjf@example.com','/uploads/avatar/8ff59cde-3109-4e0a-b440-7b4fcefe135b.jpeg','cjf',1,NULL,'',1,'2026-04-21 15:25:09','2026-11-04 01:08:34','ADMIN'),(8,'cjf1','$2a$10$ANMMW0tvJcPA0XhS4LAx4.XHUgP.FZAn57tVxIMLCz3x4CuGktQky','cjf1@example.com','http://localhost:8080/uploads/avatar/833a2598-9a33-4283-9bb3-be80824a6436.png','cjf114514',0,NULL,'',1,'2026-04-21 22:54:42','2026-11-04 01:08:34','USER'),(9,'cjf2','$2a$10$ANMMW0tvJcPA0XhS4LAx4.XHUgP.FZAn57tVxIMLCz3x4CuGktQky','cjf2@example.com','http://localhost:8080/uploads/avatar/647aac2d-211e-4bf3-b2fd-396bc8e2da30.png','',0,NULL,NULL,1,'2026-04-21 22:57:56','2026-11-04 01:08:34','USER'),(18,'cjf5','$2a$10$ANMMW0tvJcPA0XhS4LAx4.XHUgP.FZAn57tVxIMLCz3x4CuGktQky','cjf5@example.com',NULL,'',0,NULL,NULL,1,'2026-06-04 09:18:05','2026-11-04 01:08:34','USER');
/*!40000 ALTER TABLE `user` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `user_activity`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_activity` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `content` varchar(500) DEFAULT NULL,
  `create_time` datetime(6) DEFAULT NULL,
  `target_id` bigint DEFAULT NULL,
  `target_name` varchar(255) DEFAULT NULL,
  `type` varchar(50) NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `user_activity` WRITE;
/*!40000 ALTER TABLE `user_activity` DISABLE KEYS */;
INSERT INTO `user_activity` VALUES (1,NULL,'2026-04-26 01:53:35.202289',2,'七里香','PLAY',7),(2,NULL,'2026-04-26 01:53:38.055743',3,'稻香','PLAY',7);
/*!40000 ALTER TABLE `user_activity` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `user_block`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_block` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `blocked_id` bigint NOT NULL,
  `blocker_id` bigint NOT NULL,
  `create_time` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKtn2g2sexr2nbc612n8714c9mw` (`blocker_id`,`blocked_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `user_block` WRITE;
/*!40000 ALTER TABLE `user_block` DISABLE KEYS */;
/*!40000 ALTER TABLE `user_block` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `user_follow`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_follow` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `create_time` datetime(6) DEFAULT NULL,
  `follower_id` bigint NOT NULL,
  `following_id` bigint NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `user_follow` WRITE;
/*!40000 ALTER TABLE `user_follow` DISABLE KEYS */;
INSERT INTO `user_follow` VALUES (1,'2026-04-26 01:30:35.525659',8,7),(3,'2026-05-03 22:17:25.617318',7,8),(4,'2026-05-03 22:18:34.869314',9,7),(5,'2026-05-03 23:03:12.982738',7,9);
/*!40000 ALTER TABLE `user_follow` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

