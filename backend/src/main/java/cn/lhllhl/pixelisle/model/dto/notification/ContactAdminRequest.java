package cn.lhllhl.pixelisle.model.dto.notification;

import lombok.Data;

import java.io.Serializable;

/**
 * 联系管理员请求
 */
@Data
public class ContactAdminRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long spaceId;
    private String content;
}
