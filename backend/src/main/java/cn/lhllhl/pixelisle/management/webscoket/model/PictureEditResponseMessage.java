package cn.lhllhl.pixelisle.management.webscoket.model;

import cn.lhllhl.pixelisle.model.vo.UserVo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 图片编辑响应消息（服务端发送给前端）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PictureEditResponseMessage {

    /**
     * 消息类型，例如 "INFO", "ERROR", "ENTER_EDIT", "EXIT_EDIT", "EDIT_ACTION"
     */
    private String type;

    /**
     * 信息
     */
    private String message;

    /**
     * 执行的编辑动作
     */
    private String editAction;

    /**
     * 用户信息
     */
    private UserVo user;

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
     * SAVE_COMPLETE 时携带的新图片 URL
     */
    private String pictureUrl;

    /**
     * SAVE_IMAGE / CROP_CHANGE 时的裁切参数
     */
    private Map<String, Object> cropParams;

    /**
     * OBJECT_ADDED 时携带的对象序列化 JSON（阶段5）
     */
    private String objectJSON;

    /**
     * 服务端消息时间戳
     */
    private Long timestamp;
}