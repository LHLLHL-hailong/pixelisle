package cn.lhllhl.pixelisle.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 通知类型枚举
 */
@Getter
public enum NotificationTypeEnum {

    INVITATION("团队邀请", "INVITATION"),
    CONTACT_ADMIN("联系管理员", "CONTACT_ADMIN");

    private final String text;
    private final String value;

    NotificationTypeEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    public static NotificationTypeEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (NotificationTypeEnum anEnum : NotificationTypeEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
