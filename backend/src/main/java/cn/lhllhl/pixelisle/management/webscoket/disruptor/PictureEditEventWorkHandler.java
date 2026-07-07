package cn.lhllhl.pixelisle.management.webscoket.disruptor;

import cn.hutool.json.JSONUtil;

import cn.lhllhl.pixelisle.management.webscoket.PictureEditHandler;
import cn.lhllhl.pixelisle.management.webscoket.PictureEditStateService;
import cn.lhllhl.pixelisle.management.webscoket.model.PictureEditMessageTypeEnum;
import cn.lhllhl.pixelisle.management.webscoket.model.PictureEditRequestMessage;
import cn.lhllhl.pixelisle.management.webscoket.model.PictureEditResponseMessage;
import cn.lhllhl.pixelisle.model.entity.User;
import cn.lhllhl.pixelisle.service.UserService;
import com.lmax.disruptor.WorkHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.Resource;

@Slf4j
@Component
public class PictureEditEventWorkHandler implements WorkHandler<PictureEditEvent> {

    @Resource
    @Lazy
    private PictureEditHandler pictureEditHandler;

    @Resource
    private UserService userService;

    @Resource
    private PictureEditStateService pictureEditStateService;

    @Override
    public void onEvent(PictureEditEvent event) throws Exception {
        PictureEditRequestMessage req = event.getPictureEditRequestMessage();
        WebSocketSession session = event.getSession();
        User user = event.getUser();
        Long pictureId = event.getPictureId();

        String type = req.getType();
        PictureEditMessageTypeEnum msgType = PictureEditMessageTypeEnum.valueOf(type);

        switch (msgType) {
            case ENTER_EDIT:
                pictureEditHandler.handleEnterEditMessage(req, session, user, pictureId);
                break;
            case EDIT_ACTION:
                pictureEditHandler.handleEditActionMessage(req, session, user, pictureId);
                break;
            case EXIT_EDIT:
                pictureEditHandler.handleExitEditMessage(req, session, user, pictureId);
                break;

            // ── 协同编辑操作：广播 + 更新快照 ──
            case OBJECT_MODIFIED:
            case OBJECT_ADDED:
            case OBJECT_REMOVED:
            case CROP_CHANGE:
                broadcastToPicture(pictureId, session, user, msgType, req);
                updateSnapshot(pictureId, type, req);
                break;

            // ── 纯广播（不改变画布状态）──
            case OBJECT_MOVING:
            case OBJECT_SCALING:
            case OBJECT_ROTATING:
            case CANVAS_ZOOM:
            case CANVAS_PAN:
            case OBJECT_SELECTED:
            case OBJECT_DESELECTED:
                broadcastToPicture(pictureId, session, user, msgType, req);
                break;

            // ── 保存图片 ──
            case SAVE_IMAGE:
                handleSaveImage(req, session, user, pictureId);
                break;

            // ── 心跳 ──
            case PING:
                pictureEditStateService.heartbeat(pictureId);
                sendToSession(session, buildResponse(PictureEditMessageTypeEnum.PONG, user, "pong"));
                break;

            default:
                PictureEditResponseMessage err = new PictureEditResponseMessage();
                err.setType(PictureEditMessageTypeEnum.ERROR.getValue());
                err.setMessage("消息类型错误");
                err.setUser(userService.getUserVO(user));
                session.sendMessage(new TextMessage(JSONUtil.toJsonStr(err)));
        }
    }

    /** 广播 + 更新服务端快照 */
    private void broadcastToPicture(Long pictureId, WebSocketSession excludeSession,
                                     User user, PictureEditMessageTypeEnum type,
                                     PictureEditRequestMessage req) throws Exception {
        PictureEditResponseMessage res = new PictureEditResponseMessage();
        res.setType(type.getValue());
        res.setEditAction(req.getEditAction());
        res.setTargetId(req.getTargetId());
        res.setParams(req.getParams());
        res.setCropParams(req.getCropParams());
        res.setObjectJSON(req.getObjectJSON());
        res.setTimestamp(System.currentTimeMillis());
        res.setUser(userService.getUserVO(user));

        pictureEditHandler.broadcastToPictureExclude(pictureId, res, excludeSession);
    }

    /** 更新服务端快照（try-catch 保护，不影响协同） */
    private void updateSnapshot(Long pictureId, String type, PictureEditRequestMessage req) {
        try {
            pictureEditStateService.updateSnapshot(
                    pictureId, type, req.getEditAction(),
                    req.getTargetId(), req.getObjectJSON(), req.getCropParams());
        } catch (Exception e) {
            log.error("[Disruptor] updateSnapshot failed for pictureId={} type={}", pictureId, type, e);
        }
    }

    /** 处理保存请求 */
    private void handleSaveImage(PictureEditRequestMessage req, WebSocketSession session,
                                  User user, Long pictureId) throws Exception {
        pictureEditHandler.handleSaveImageMessage(req, session, user, pictureId);
    }

    private void sendToSession(WebSocketSession session, PictureEditResponseMessage res) {
        try {
            if (session.isOpen()) {
                synchronized (session) {
                    session.sendMessage(new TextMessage(JSONUtil.toJsonStr(res)));
                }
            }
        } catch (Exception e) {
            log.error("Failed to send message to session", e);
        }
    }

    private PictureEditResponseMessage buildResponse(PictureEditMessageTypeEnum type, User user, String message) {
        PictureEditResponseMessage res = new PictureEditResponseMessage();
        res.setType(type.getValue());
        res.setMessage(message);
        res.setUser(userService.getUserVO(user));
        res.setTimestamp(System.currentTimeMillis());
        return res;
    }
}
