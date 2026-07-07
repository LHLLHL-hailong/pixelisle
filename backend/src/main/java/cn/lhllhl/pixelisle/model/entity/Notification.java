package cn.lhllhl.pixelisle.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 通知表
 * @TableName notification
 */
@TableName(value = "notification")
@Data
public class Notification implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 通知类型：INVITATION | CONTACT_ADMIN
     */
    private String type;

    /**
     * 发送方用户ID
     */
    private Long senderId;

    /**
     * 接收方用户ID
     */
    private Long receiverId;

    /**
     * 关联空间ID
     */
    private Long spaceId;

    /**
     * INVITATION 时的邀请角色
     */
    private String invitedRole;

    /**
     * 内容/留言
     */
    private String content;

    /**
     * 扩展数据 JSON
     */
    private String metadata;

    /**
     * 状态：PENDING|ACCEPTED|REJECTED|EXPIRED|UNREAD|READ
     */
    private String status;

    /**
     * 已读标记
     */
    private Integer isRead;

    /**
     * 阅读时间
     */
    private Date readTime;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
