package cn.lhllhl.pixelisle.controller;

import cn.lhllhl.pixelisle.common.BaseResponse;
import cn.lhllhl.pixelisle.common.ResultUtils;
import cn.lhllhl.pixelisle.exception.ErrorCode;
import cn.lhllhl.pixelisle.exception.ThrowUtils;
import cn.lhllhl.pixelisle.model.dto.notification.*;
import cn.lhllhl.pixelisle.model.entity.Notification;
import cn.lhllhl.pixelisle.model.entity.User;
import cn.lhllhl.pixelisle.service.NotificationService;
import cn.lhllhl.pixelisle.service.UserService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/notification")
@Slf4j
@Api(tags = "通知接口")
public class NotificationController {

    @Resource
    private NotificationService notificationService;

    @Resource
    private UserService userService;

    /**
     * 发起团队邀请
     */
    @PostMapping("/invitation/send")
    @ApiOperation("发起团队邀请")
    public BaseResponse<Long> sendInvitation(@RequestBody InvitationSendRequest request,
                                              HttpServletRequest httpRequest) {
        ThrowUtils.throwIf(request == null || request.getSpaceId() == null || request.getReceiverId() == null,
                ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(httpRequest);
        Long id = notificationService.sendInvitation(loginUser, request.getSpaceId(),
                request.getReceiverId(), request.getInvitedRole(), request.getContent());
        return ResultUtils.success(id);
    }

    /**
     * 接受邀请
     */
    @PostMapping("/invitation/accept")
    @ApiOperation("接受团队邀请")
    public BaseResponse<Boolean> acceptInvitation(@RequestBody InvitationActionRequest request,
                                                   HttpServletRequest httpRequest) {
        ThrowUtils.throwIf(request == null || request.getNotificationId() == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(httpRequest);
        notificationService.acceptInvitation(loginUser, request.getNotificationId());
        return ResultUtils.success(true);
    }

    /**
     * 拒绝邀请
     */
    @PostMapping("/invitation/reject")
    @ApiOperation("拒绝团队邀请")
    public BaseResponse<Boolean> rejectInvitation(@RequestBody InvitationActionRequest request,
                                                   HttpServletRequest httpRequest) {
        ThrowUtils.throwIf(request == null || request.getNotificationId() == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(httpRequest);
        notificationService.rejectInvitation(loginUser, request.getNotificationId());
        return ResultUtils.success(true);
    }

    /**
     * 回复通知（多轮交互）
     */
    @PostMapping("/reply")
    @ApiOperation("回复通知")
    public BaseResponse<Boolean> reply(@RequestBody ReplyRequest request,
                                        HttpServletRequest httpRequest) {
        ThrowUtils.throwIf(request == null || request.getNotificationId() == null,
                ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(httpRequest);
        notificationService.reply(loginUser, request.getNotificationId(), request.getContent());
        return ResultUtils.success(true);
    }

    /**
     * 联系管理员
     */
    @PostMapping("/contactAdmin")
    @ApiOperation("联系管理员留言")
    public BaseResponse<Boolean> contactAdmin(@RequestBody ContactAdminRequest request,
                                               HttpServletRequest httpRequest) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(httpRequest);
        notificationService.contactAdmin(loginUser, request.getSpaceId(), request.getContent());
        return ResultUtils.success(true);
    }

    /**
     * 查询通知列表
     */
    @PostMapping("/list")
    @ApiOperation("查询通知列表")
    public BaseResponse<Page<Notification>> listNotifications(@RequestBody NotificationQueryRequest request,
                                                                HttpServletRequest httpRequest) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(httpRequest);
        Page<Notification> page = notificationService.listNotifications(loginUser.getId(),
                request.getType(), request.getStatus(), request.getIsRead(),
                request.getCurrent(), request.getPageSize());
        return ResultUtils.success(page);
    }

    /**
     * 未读数量
     */
    @GetMapping("/unreadCount")
    @ApiOperation("获取未读通知数量")
    public BaseResponse<Map<String, Object>> unreadCount(HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);
        long total = notificationService.getUnreadCount(loginUser.getId());
        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        return ResultUtils.success(result);
    }
}
