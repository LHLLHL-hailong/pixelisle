package cn.lhllhl.pixelisle.model.dto.notification;

import lombok.Data;

import java.io.Serializable;

/**
 * 邀请发送请求
 */
@Data
public class InvitationSendRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long spaceId;
    private Long receiverId;
    private String invitedRole;
    private String content;
}
