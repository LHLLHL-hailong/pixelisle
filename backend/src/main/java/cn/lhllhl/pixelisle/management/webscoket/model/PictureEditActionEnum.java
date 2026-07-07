package cn.lhllhl.pixelisle.management.webscoket.model;

import lombok.Getter;

@Getter
public enum PictureEditActionEnum {

    ZOOM_IN("放大操作", "ZOOM_IN"),
    ZOOM_OUT("缩小操作", "ZOOM_OUT"),
    ROTATE_LEFT("左旋操作", "ROTATE_LEFT"),
    ROTATE_RIGHT("右旋操作", "ROTATE_RIGHT"),

    // ── 阶段2新增 ──
    CROP_MOVE("裁切框移动", "CROP_MOVE"),
    CROP_RESIZE("裁切框调整", "CROP_RESIZE"),
    DRAW_PATH("画笔路径", "DRAW_PATH"),
    ADD_TEXT("添加文字", "ADD_TEXT"),
    ADD_RECT("添加矩形", "ADD_RECT"),
    APPLY_FILTER("应用滤镜", "APPLY_FILTER"),
    DELETE_OBJECT("删除对象", "DELETE_OBJECT"),
    UNDO("撤销", "UNDO"),
    REDO("重做", "REDO");

    private final String text;
    private final String value;

    PictureEditActionEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举
     */
    public static PictureEditActionEnum getEnumByValue(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        for (PictureEditActionEnum actionEnum : PictureEditActionEnum.values()) {
            if (actionEnum.value.equals(value)) {
                return actionEnum;
            }
        }
        return null;
    }
}
