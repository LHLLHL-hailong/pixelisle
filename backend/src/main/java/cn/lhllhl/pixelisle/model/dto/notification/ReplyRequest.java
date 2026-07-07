package cn.lhllhl.pixelisle.model.dto.notification;

import lombok.Data;

import java.io.Serializable;

/**
 * 回复通知请求
 */
@Data
public class ReplyRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 被回复的原通知ID */
    private Long notificationId;

    /** 回复内容 */
    private String content;
}
