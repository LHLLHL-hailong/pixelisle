package cn.lhllhl.pixelisle.management.webscoket.model;

import lombok.Getter;

@Getter
public enum PictureEditMessageTypeEnum {

    INFO("发送通知", "INFO"),
    ERROR("发送错误", "ERROR"),
    ENTER_EDIT("进入编辑状态", "ENTER_EDIT"),
    EXIT_EDIT("退出编辑状态", "EXIT_EDIT"),
    EDIT_ACTION("执行编辑操作", "EDIT_ACTION"),

    // ── 阶段2新增：协同编辑消息类型 ──
    OBJECT_MODIFIED("对象修改完成", "OBJECT_MODIFIED"),
    OBJECT_MOVING("对象移动中", "OBJECT_MOVING"),
    OBJECT_SCALING("对象缩放中", "OBJECT_SCALING"),
    OBJECT_ROTATING("对象旋转中", "OBJECT_ROTATING"),
    OBJECT_ADDED("对象新增", "OBJECT_ADDED"),
    OBJECT_REMOVED("对象删除", "OBJECT_REMOVED"),
    CANVAS_ZOOM("画布缩放", "CANVAS_ZOOM"),
    CANVAS_PAN("画布平移", "CANVAS_PAN"),
    CROP_CHANGE("裁切框变化", "CROP_CHANGE"),
    OBJECT_SELECTED("对象被选中", "OBJECT_SELECTED"),
    OBJECT_DESELECTED("对象取消选中", "OBJECT_DESELECTED"),
    SAVE_IMAGE("请求保存图片", "SAVE_IMAGE"),
    SAVE_COMPLETE("保存完成", "SAVE_COMPLETE"),
    SYNC_START("同步开始", "SYNC_START"),
    SYNC_HISTORY("历史消息", "SYNC_HISTORY"),
    SYNC_END("同步结束", "SYNC_END"),
    PING("心跳", "PING"),
    PONG("心跳响应", "PONG");

    private final String text;
    private final String value;

    PictureEditMessageTypeEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举
     */
    public static PictureEditMessageTypeEnum getEnumByValue(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        for (PictureEditMessageTypeEnum typeEnum : PictureEditMessageTypeEnum.values()) {
            if (typeEnum.value.equals(value)) {
                return typeEnum;
            }
        }
        return null;
    }
}
