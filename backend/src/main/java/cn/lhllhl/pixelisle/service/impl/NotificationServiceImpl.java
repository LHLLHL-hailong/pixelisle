package cn.lhllhl.pixelisle.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.lhllhl.pixelisle.exception.BusinessException;
import cn.lhllhl.pixelisle.exception.ErrorCode;
import cn.lhllhl.pixelisle.exception.ThrowUtils;
import cn.lhllhl.pixelisle.mapper.NotificationMapper;
import cn.lhllhl.pixelisle.model.entity.Notification;
import cn.lhllhl.pixelisle.model.entity.Space;
import cn.lhllhl.pixelisle.model.entity.SpaceUser;
import cn.lhllhl.pixelisle.model.entity.User;
import cn.lhllhl.pixelisle.model.enums.NotificationStatusEnum;
import cn.lhllhl.pixelisle.model.enums.NotificationTypeEnum;
import cn.lhllhl.pixelisle.model.enums.SpaceRoleEnum;
import cn.lhllhl.pixelisle.model.enums.SpaceTypeEnum;
import cn.lhllhl.pixelisle.service.NotificationService;
import cn.lhllhl.pixelisle.service.SpaceService;
import cn.lhllhl.pixelisle.service.SpaceUserService;
import cn.lhllhl.pixelisle.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 通知服务实现
 */
@Service
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification>
        implements NotificationService {

    @Autowired
    private UserService userService;

    @Autowired
    @Lazy
    private SpaceService spaceService;

    @Autowired
    private SpaceUserService spaceUserService;

    @Override
    public Long sendInvitation(User sender, Long spaceId, Long receiverId, String invitedRole, String content) {
        ThrowUtils.throwIf(sender == null, ErrorCode.NOT_LOGIN_ERROR);
        ThrowUtils.throwIf(spaceId == null || receiverId == null, ErrorCode.PARAMS_ERROR);

        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");

        // 仅团队空间走邀请流程
        if (SpaceTypeEnum.TEAM.getValue() != space.getSpaceType()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "仅团队空间支持邀请成员");
        }

        // 校验被邀请用户存在
        User receiver = userService.getById(receiverId);
        ThrowUtils.throwIf(receiver == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");

        // 校验是否已在空间中
        List<SpaceUser> existingMembers = spaceUserService.list(
                new QueryWrapper<SpaceUser>().eq("spaceId", spaceId).eq("userId", receiverId));
        if (!existingMembers.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该用户已在空间中");
        }

        // 检查是否有 PENDING 邀请
        long pendingCount = this.count(new QueryWrapper<Notification>()
                .eq("type", NotificationTypeEnum.INVITATION.getValue())
                .eq("spaceId", spaceId)
                .eq("receiverId", receiverId)
                .eq("status", NotificationStatusEnum.PENDING.getValue()));
        if (pendingCount > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "已发送过邀请，等待确认");
        }

        if (StrUtil.isBlank(invitedRole)) {
            invitedRole = SpaceRoleEnum.VIEWER.getValue();
        }

        Notification notification = new Notification();
        notification.setType(NotificationTypeEnum.INVITATION.getValue());
        notification.setSenderId(sender.getId());
        notification.setReceiverId(receiverId);
        notification.setSpaceId(spaceId);
        notification.setInvitedRole(invitedRole);
        notification.setContent(content);
        notification.setStatus(NotificationStatusEnum.PENDING.getValue());
        notification.setIsRead(0);
        notification.setCreateTime(new Date());
        notification.setUpdateTime(new Date());

        boolean saved = this.save(notification);
        ThrowUtils.throwIf(!saved, ErrorCode.OPERATION_ERROR);

        return notification.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void acceptInvitation(User receiver, Long notificationId) {
        ThrowUtils.throwIf(receiver == null, ErrorCode.NOT_LOGIN_ERROR);

        Notification notification = this.getById(notificationId);
        ThrowUtils.throwIf(notification == null, ErrorCode.NOT_FOUND_ERROR, "通知不存在");

        if (!receiver.getId().equals(notification.getReceiverId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        if (!NotificationStatusEnum.PENDING.getValue().equals(notification.getStatus())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该邀请已被处理");
        }

        // 检查是否已加入空间（防并发）
        List<SpaceUser> existingMembers = spaceUserService.list(new QueryWrapper<SpaceUser>()
                .eq("spaceId", notification.getSpaceId()).eq("userId", receiver.getId()));
        if (!existingMembers.isEmpty()) {
            notification.setStatus(NotificationStatusEnum.ACCEPTED.getValue());
            notification.setUpdateTime(new Date());
            this.updateById(notification);
            return;
        }

        // 创建 SpaceUser
        SpaceUser spaceUser = new SpaceUser();
        spaceUser.setSpaceId(notification.getSpaceId());
        spaceUser.setUserId(receiver.getId());
        spaceUser.setSpaceRole(notification.getInvitedRole());
        spaceUser.setCreateTime(new Date());
        spaceUser.setUpdateTime(new Date());
        spaceUserService.save(spaceUser);

        // 更新通知状态
        notification.setStatus(NotificationStatusEnum.ACCEPTED.getValue());
        notification.setIsRead(1);
        notification.setReadTime(new Date());
        notification.setUpdateTime(new Date());
        this.updateById(notification);

        // 给管理员发通知
        notifySender(notification.getSenderId(), notification.getSpaceId(),
                NotificationTypeEnum.INVITATION.getValue(),
                receiver.getUserName() + " 已接受你的团队邀请",
                NotificationStatusEnum.ACCEPTED.getValue());
    }

    @Override
    public void rejectInvitation(User receiver, Long notificationId) {
        ThrowUtils.throwIf(receiver == null, ErrorCode.NOT_LOGIN_ERROR);

        Notification notification = this.getById(notificationId);
        ThrowUtils.throwIf(notification == null, ErrorCode.NOT_FOUND_ERROR, "通知不存在");

        if (!receiver.getId().equals(notification.getReceiverId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        if (!NotificationStatusEnum.PENDING.getValue().equals(notification.getStatus())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该邀请已被处理");
        }

        notification.setStatus(NotificationStatusEnum.REJECTED.getValue());
        notification.setIsRead(1);
        notification.setReadTime(new Date());
        notification.setUpdateTime(new Date());
        this.updateById(notification);

        // 给管理员发通知
        User receiverUser = userService.getById(receiver.getId());
        String name = receiverUser != null ? receiverUser.getUserName() : "用户";
        notifySender(notification.getSenderId(), notification.getSpaceId(),
                NotificationTypeEnum.INVITATION.getValue(),
                name + " 已拒绝你的团队邀请",
                NotificationStatusEnum.REJECTED.getValue());
    }

    @Override
    public void contactAdmin(User sender, Long spaceId, String content) {
        ThrowUtils.throwIf(sender == null, ErrorCode.NOT_LOGIN_ERROR);
        ThrowUtils.throwIf(StrUtil.isBlank(content), ErrorCode.PARAMS_ERROR, "留言内容不能为空");

        // 查找空间的所有管理员
        List<SpaceUser> admins = spaceUserService.list(new QueryWrapper<SpaceUser>()
                .eq(spaceId != null, "spaceId", spaceId)
                .eq("spaceRole", SpaceRoleEnum.ADMIN.getValue()));

        if (admins.isEmpty()) {
            // fallback：发给系统管理员
            List<User> adminUsers = userService.list(new QueryWrapper<User>()
                    .eq("userRole", "admin"));
            for (User admin : adminUsers) {
                createContactNotification(sender.getId(), admin.getId(), spaceId, content);
            }
            return;
        }

        for (SpaceUser admin : admins) {
            createContactNotification(sender.getId(), admin.getUserId(), spaceId, content);
        }
    }

    private void createContactNotification(Long senderId, Long receiverId, Long spaceId, String content) {
        Notification notification = new Notification();
        notification.setType(NotificationTypeEnum.CONTACT_ADMIN.getValue());
        notification.setSenderId(senderId);
        notification.setReceiverId(receiverId);
        notification.setSpaceId(spaceId);
        notification.setContent(content);
        notification.setStatus(NotificationStatusEnum.UNREAD.getValue());
        notification.setIsRead(0);
        notification.setCreateTime(new Date());
        notification.setUpdateTime(new Date());
        this.save(notification);
    }

    private void notifySender(Long receiverId, Long spaceId, String type, String content, String status) {
        Notification notification = new Notification();
        notification.setType(type);
        notification.setSenderId(0L); // 系统消息
        notification.setReceiverId(receiverId);
        notification.setSpaceId(spaceId);
        notification.setContent(content);
        notification.setStatus(status);
        notification.setIsRead(0);
        notification.setCreateTime(new Date());
        notification.setUpdateTime(new Date());
        this.save(notification);
    }

    @Override
    public void reply(User replier, Long notificationId, String content) {
        ThrowUtils.throwIf(replier == null, ErrorCode.NOT_LOGIN_ERROR);
        ThrowUtils.throwIf(StrUtil.isBlank(content), ErrorCode.PARAMS_ERROR, "回复内容不能为空");

        Notification original = this.getById(notificationId);
        ThrowUtils.throwIf(original == null, ErrorCode.NOT_FOUND_ERROR, "原通知不存在");

        // 校验当前用户是原通知的接收者
        if (!replier.getId().equals(original.getReceiverId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        Notification reply = new Notification();
        reply.setType(original.getType());
        reply.setSenderId(replier.getId());
        reply.setReceiverId(original.getSenderId());
        reply.setSpaceId(original.getSpaceId());
        reply.setContent(content);
        reply.setStatus(NotificationStatusEnum.UNREAD.getValue());
        reply.setIsRead(0);
        reply.setCreateTime(new Date());
        reply.setUpdateTime(new Date());

        boolean saved = this.save(reply);
        ThrowUtils.throwIf(!saved, ErrorCode.OPERATION_ERROR);
    }

    @Override
    public Page<Notification> listNotifications(Long userId, String type, String status, Integer isRead,
                                                  Long current, Long pageSize) {
        QueryWrapper<Notification> qw = new QueryWrapper<>();
        qw.eq("receiverId", userId);
        qw.eq(StrUtil.isNotBlank(type), "type", type);
        qw.eq(StrUtil.isNotBlank(status), "status", status);
        qw.eq(isRead != null, "isRead", isRead);
        qw.orderByDesc("createTime");

        Page<Notification> page = this.page(new Page<>(current, pageSize), qw);

        // 顺手标记已读
        for (Notification n : page.getRecords()) {
            if (n.getIsRead() == 0) {
                n.setIsRead(1);
                n.setReadTime(new Date());
                this.updateById(n);
            }
        }

        return page;
    }

    @Override
    public long getUnreadCount(Long userId) {
        return this.count(new QueryWrapper<Notification>()
                .eq("receiverId", userId)
                .eq("isRead", 0));
    }
}
