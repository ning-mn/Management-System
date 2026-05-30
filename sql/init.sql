-- 会议室预定管理系统 数据库初始化脚本
CREATE DATABASE IF NOT EXISTS meeting_room_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE meeting_room_db;

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `username` VARCHAR(50) NOT NULL,
    `password` VARCHAR(255) NOT NULL COMMENT '加密后的密码',
    `role` ENUM('employee', 'admin') NOT NULL DEFAULT 'employee',
    `real_name` VARCHAR(50) DEFAULT NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 会议室表
CREATE TABLE IF NOT EXISTS `meeting_room` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(100) NOT NULL,
    `capacity` INT NOT NULL COMMENT '容纳人数',
    `equipment` VARCHAR(255) DEFAULT NULL COMMENT '设备描述',
    `status` ENUM('available', 'maintenance') NOT NULL DEFAULT 'available',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 预定表
CREATE TABLE IF NOT EXISTS `booking` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `user_id` INT NOT NULL,
    `room_id` INT NOT NULL,
    `start_time` DATETIME NOT NULL,
    `end_time` DATETIME NOT NULL,
    `status` ENUM('pending', 'confirmed', 'cancelled', 'rejected') NOT NULL DEFAULT 'pending',
    `remark` TEXT DEFAULT NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_room_id` (`room_id`),
    KEY `idx_status` (`status`),
    KEY `idx_start_time` (`start_time`),
    KEY `idx_end_time` (`end_time`),
    CONSTRAINT `fk_booking_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_booking_room` FOREIGN KEY (`room_id`) REFERENCES `meeting_room` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 插入默认管理员账号 (密码: admin123)
-- 注意: 如果管理员已存在则跳过（防止重复执行报错）
INSERT IGNORE INTO `user` (`username`, `password`, `role`, `real_name`) VALUES
('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'admin', '系统管理员');

-- 插入示例会议室数据
INSERT IGNORE INTO `meeting_room` (`id`, `name`, `capacity`, `equipment`, `status`) VALUES
(1, '会议室A', 10, '投影仪、白板、视频会议设备', 'available'),
(2, '会议室B', 6, '投影仪、白板', 'available'),
(3, '会议室C', 20, '投影仪、音响、视频会议设备', 'available'),
(4, '会议室D', 4, '白板', 'available'),
(5, 'VIP会议室', 30, '投影仪、音响、视频会议设备、茶歇', 'available');

-- 注意：如果 admin 密码登录失败，请执行以下步骤：
-- 1. 启动后端服务
-- 2. 注册一个新的管理员账号（通过注册API）
-- 3. 或者直接在数据库中使用 SQL 更新密码：
--    UPDATE user SET password = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy' WHERE username = 'admin';