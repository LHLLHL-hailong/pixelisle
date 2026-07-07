package cn.lhllhl.pixelisle.model.dto.notification;

import lombok.Data;

import java.io.Serializable;

/**
 * 邀请接受/拒绝请求
 */
@Data
public class InvitationActionRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long notificationId;
}
