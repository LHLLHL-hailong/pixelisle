package cn.lhllhl.pixelisle.model.dto.notification;

import lombok.Data;

import java.io.Serializable;

/**
 * 通知查询请求
 */
@Data
public class NotificationQueryRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String type;
    private String status;
    private Integer isRead;
    private Long spaceId;
    private Long current = 1L;
    private Long pageSize = 10L;
}
