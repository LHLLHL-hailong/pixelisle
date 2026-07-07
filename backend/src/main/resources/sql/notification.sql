-- 统一通知表
CREATE TABLE IF NOT EXISTS notification (
    id           BIGINT       NOT NULL AUTO_INCREMENT  COMMENT 'PK',
    type         VARCHAR(32)  NOT NULL                  COMMENT 'INVITATION | CONTACT_ADMIN',
    senderId     BIGINT       NOT NULL                  COMMENT '发送方用户ID',
    receiverId   BIGINT       NOT NULL                  COMMENT '接收方用户ID',
    spaceId      BIGINT                                 COMMENT '关联空间ID',
    invitedRole  VARCHAR(32)                            COMMENT 'INVITATION时的邀请角色',
    content      VARCHAR(500)                           COMMENT '留言内容 / 邀请附言',
    metadata     JSON                                   COMMENT '扩展数据',
    status       VARCHAR(32)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING|ACCEPTED|REJECTED|EXPIRED|UNREAD|READ',
    isRead       TINYINT      NOT NULL DEFAULT 0        COMMENT '已读标记',
    readTime     DATETIME                               COMMENT '阅读时间',
    createTime   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updateTime   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    isDelete     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    INDEX idx_receiver_type_status (receiverId, type, status),
    INDEX idx_sender (senderId),
    INDEX idx_space (spaceId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='统一通知表';
