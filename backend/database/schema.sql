-- 创建数据库
CREATE DATABASE IF NOT EXISTS pixel_isle;
USE pixel_isle;

-- 用户表
CREATE TABLE IF NOT EXISTS user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'id',
    user_account VARCHAR(256) NOT NULL COMMENT '账号',
    user_password VARCHAR(512) NOT NULL COMMENT '密码',
    user_name VARCHAR(256) COMMENT '用户名称',
    user_avatar VARCHAR(1024) COMMENT '用户头像',
    user_profile VARCHAR(512) COMMENT '用户简介',
    user_role VARCHAR(256) NOT NULL DEFAULT 'user' COMMENT '用户角色，user/admin',
    edit_time DATETIME NULL COMMENT '编辑时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    is_delete TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除',
    vip_expire_time DATETIME COMMENT '会员过期时间',
    vip_code VARCHAR(256) COMMENT '会员兑换码',
    vip_number BIGINT COMMENT '会员编号',
    INDEX idx_user_account (user_account),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 空间表
CREATE TABLE IF NOT EXISTS space (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'id',
    space_name VARCHAR(128) NOT NULL COMMENT '空间名称',
    space_level INT COMMENT '空间级别，0-普通，1-专业，2-旗舰',
    max_size BIGINT COMMENT '空间图片的最大大小',
    max_count BIGINT COMMENT '空间图片的最大数量',
    total_size BIGINT NOT NULL DEFAULT 0 COMMENT '当前空间图片的总大小',
    total_count BIGINT NOT NULL DEFAULT 0 COMMENT '当前空间下的图片数量',
    user_id BIGINT NOT NULL COMMENT '创建用户 id',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    edit_time DATETIME NULL COMMENT '编辑时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    is_delete TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除',
    space_type TINYINT NOT NULL DEFAULT 0 COMMENT '空间类型，0-私有，1-团队',
    INDEX idx_user_id (user_id),
    INDEX idx_space_level (space_level),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='空间表';

-- 图片表
CREATE TABLE IF NOT EXISTS picture (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'id',
    url VARCHAR(1024) NOT NULL COMMENT '图片 url',
    name VARCHAR(256) NOT NULL COMMENT '图片名称',
    introduction VARCHAR(512) COMMENT '简介',
    category VARCHAR(64) COMMENT '分类',
    tags TEXT COMMENT '标签，JSON 数组',
    pic_size BIGINT COMMENT '图片大小',
    pic_width INT COMMENT '图片宽度',
    pic_height INT COMMENT '图片高度',
    pic_scale DOUBLE COMMENT '图片宽高比例',
    pic_format VARCHAR(32) COMMENT '图片格式',
    user_id BIGINT NOT NULL COMMENT '创建用户 id',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    edit_time DATETIME NULL COMMENT '编辑时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    is_delete TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除',
    review_status INT NOT NULL DEFAULT 0 COMMENT '审核状态 0--待审核 1--通过 2--拒绝',
    review_message VARCHAR(512) COMMENT '审核信息',
    reviewer_id BIGINT COMMENT '审核人ID',
    review_time DATETIME NULL COMMENT '审核时间',
    thumbnail_url VARCHAR(1024) COMMENT '缩略图url',
    space_id BIGINT COMMENT '空间 id（为空表示公共图库）',
    pic_color VARCHAR(32) COMMENT '图片主色调',
    INDEX idx_user_id (user_id),
    INDEX idx_space_id (space_id),
    INDEX idx_category (category),
    INDEX idx_review_status (review_status),
    INDEX idx_create_time (create_time),
    INDEX idx_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图片表';

-- 添加外键约束
ALTER TABLE space ADD CONSTRAINT fk_space_user_id FOREIGN KEY (user_id) REFERENCES user(id);
ALTER TABLE picture ADD CONSTRAINT fk_picture_user_id FOREIGN KEY (user_id) REFERENCES user(id);
ALTER TABLE picture ADD CONSTRAINT fk_picture_space_id FOREIGN KEY (space_id) REFERENCES space(id);