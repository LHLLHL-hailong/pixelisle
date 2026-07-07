package cn.lhllhl.pixelisle.service;

import cn.lhllhl.pixelisle.model.entity.Notification;
import cn.lhllhl.pixelisle.model.entity.User;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 通知服务接口
 */
public interface NotificationService extends IService<Notification> {

    /**
     * 发送团队邀请
     */
    Long sendInvitation(User sender, Long spaceId, Long receiverId, String invitedRole, String content);

    /**
     * 接受邀请
     */
    void acceptInvitation(User receiver, Long notificationId);

    /**
     * 拒绝邀请
     */
    void rejectInvitation(User receiver, Long notificationId);

    /**
     * 联系管理员
     */
    void contactAdmin(User sender, Long spaceId, String content);

    /**
     * 回复通知（CONTACT_ADMIN 类型的多轮交互）
     */
    void reply(User replier, Long notificationId, String content);

    /**
     * 分页查询通知列表
     */
    Page<Notification> listNotifications(Long userId, String type, String status, Integer isRead, Long current, Long pageSize);

    /**
     * 获取未读数量
     */
    long getUnreadCount(Long userId);
}
