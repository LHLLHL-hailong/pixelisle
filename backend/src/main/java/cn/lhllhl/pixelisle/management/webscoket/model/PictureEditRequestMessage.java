package cn.lhllhl.pixelisle.management.webscoket.model;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 前端发送进服务端
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PictureEditRequestMessage {

    /**
     * 消息类型，例如 "ENTER_EDIT", "EXIT_EDIT", "EDIT_ACTION"
     */
    private String type;

    /**
     * 执行的编辑动作
     */
    private String editAction;

    // ── 阶段2新增字段 ──

    /**
     * 被操作对象的 customId
     */
    private String targetId;

    /**
     * 操作参数 (left, top, scaleX, scaleY, angle, zoom, ...)
     */
    private Map<String, Object> params;

    /**
     * OBJECT_ADDED 时携带的对象序列化 JSON
     */
    private String objectJSON;

    /**
     * SAVE_IMAGE / CROP_CHANGE 时携带裁切参数
     */
    private Map<String, Object> cropParams;

    /**
     * 客户端时间戳 (用于 LWW 冲突解决)
     */
    private Long timestamp;
}
