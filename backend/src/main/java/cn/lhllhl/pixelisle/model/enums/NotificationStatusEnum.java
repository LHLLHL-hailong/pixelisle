package cn.lhllhl.pixelisle.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 通知状态枚举
 */
@Getter
public enum NotificationStatusEnum {

    /** 邀请状态 */
    PENDING("待处理", "PENDING"),
    ACCEPTED("已接受", "ACCEPTED"),
    REJECTED("已拒绝", "REJECTED"),
    EXPIRED("已过期", "EXPIRED"),

    /** 通用阅读状态（CONTACT_ADMIN 等非邀请类型使用） */
    UNREAD("未读", "UNREAD"),
    READ("已读", "READ");

    private final String text;
    private final String value;

    NotificationStatusEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    public static NotificationStatusEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (NotificationStatusEnum anEnum : NotificationStatusEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
