package cn.lhllhl.pixelisle.management.webscoket;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import cn.lhllhl.pixelisle.management.webscoket.disruptor.PictureEditEventProducer;
import cn.lhllhl.pixelisle.management.webscoket.model.PictureEditActionEnum;
import cn.lhllhl.pixelisle.management.webscoket.model.PictureEditMessageTypeEnum;
import cn.lhllhl.pixelisle.management.webscoket.model.PictureEditRequestMessage;
import cn.lhllhl.pixelisle.management.webscoket.model.PictureEditResponseMessage;
import cn.lhllhl.pixelisle.model.entity.User;
import cn.lhllhl.pixelisle.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 图片编辑处理器
 */
@Slf4j
@Component
public class PictureEditHandler extends TextWebSocketHandler {


    // 保存所有连接的会话，key: pictureId, value: 用户会话集合
    private final Map<Long, Set<WebSocketSession>> pictureSessions = new ConcurrentHashMap<>();


    @Autowired
    private  UserService userService;

    @Autowired
    private PictureEditEventProducer pictureEditEventProducer;

    @Autowired
    private PictureEditStateService pictureEditStateService;

    /**·····
     * 连接建立成功之后
     * @param session
     * @throws Exception
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {

        User user = (User)session.getAttributes().get("user");
        Long pictureId = (Long) session.getAttributes().get("pictureId");

        pictureSessions.putIfAbsent(pictureId,ConcurrentHashMap.newKeySet());
        pictureSessions.get(pictureId).add(session);

        // ★ 阶段4：新用户同步 — 发送历史操作让新用户追上当前状态
        sendSyncData(session, pictureId);

        PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
        pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.INFO.getValue());

        String msg=String.format("用户 %s 加入编辑",user.getUserName());
        pictureEditResponseMessage.setMessage(msg);
        pictureEditResponseMessage.setUser(userService.getUserVO(user));

        //也包括自己的广播
        broadcastToPicture(pictureId, pictureEditResponseMessage);

        super.afterConnectionEstablished(session);
    }

    /** 向新连接发送同步数据（服务端快照，一次发送） */
    private void sendSyncData(WebSocketSession session, Long pictureId) {
        try {
            String snapshot = pictureEditStateService.getSnapshotForSync(pictureId);
            if (snapshot == null) {
                // 无快照 → 直接结束同步
                PictureEditResponseMessage syncEnd = new PictureEditResponseMessage();
                syncEnd.setType(PictureEditMessageTypeEnum.SYNC_END.getValue());
                syncEnd.setMessage("同步完成（无历史）");
                syncEnd.setTimestamp(System.currentTimeMillis());
                sendToSession(session, syncEnd);
                return;
            }

            // SYNC_START
            PictureEditResponseMessage syncStart = new PictureEditResponseMessage();
            syncStart.setType(PictureEditMessageTypeEnum.SYNC_START.getValue());
            syncStart.setMessage("开始同步编辑状态");
            syncStart.setTimestamp(System.currentTimeMillis());
            sendToSession(session, syncStart);

            // 发送快照（type=SYNC_HISTORY，前端 applyRemoteEdit 处理）
            PictureEditResponseMessage snapMsg = new PictureEditResponseMessage();
            snapMsg.setType(PictureEditMessageTypeEnum.SYNC_HISTORY.getValue());
            snapMsg.setEditAction("SNAPSHOT");
            snapMsg.setObjectJSON(snapshot);
            snapMsg.setTimestamp(System.currentTimeMillis());
            sendToSession(session, snapMsg);

            // SYNC_END
            PictureEditResponseMessage syncEnd = new PictureEditResponseMessage();
            syncEnd.setType(PictureEditMessageTypeEnum.SYNC_END.getValue());
            syncEnd.setMessage("同步完成");
            syncEnd.setTimestamp(System.currentTimeMillis());
            sendToSession(session, syncEnd);
        } catch (Exception e) {
            log.error("Failed to send sync data for picture {}", pictureId, e);
        }
    }

    /** 发送消息给单个 WebSocket session */
    private void sendToSession(WebSocketSession session, PictureEditResponseMessage res) throws Exception {
        if (session.isOpen()) {
            String str = toJsonString(res);
            synchronized (session) {
                session.sendMessage(new TextMessage(str));
            }
        }
    }

    /** 序列化响应消息为 JSON（Long → String 防精度丢失） */
    private String toJsonString(PictureEditResponseMessage res) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addSerializer(Long.class, ToStringSerializer.instance);
        module.addSerializer(Long.TYPE, ToStringSerializer.instance);
        objectMapper.registerModule(module);
        return objectMapper.writeValueAsString(res);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 将消息解析为 PictureEditMessage
        PictureEditRequestMessage pictureEditRequestMessage = JSONUtil.toBean(message.getPayload(), PictureEditRequestMessage.class);
        String type = pictureEditRequestMessage.getType();
        PictureEditMessageTypeEnum pictureEditMessageTypeEnum = PictureEditMessageTypeEnum.valueOf(type);

        // 从 Session 属性中获取公共参数
        Map<String, Object> attributes = session.getAttributes();
        User user = (User) attributes.get("user");
        Long pictureId = (Long) attributes.get("pictureId");

        pictureEditEventProducer.publishEvent(pictureEditRequestMessage,session,user,pictureId);

//        // 调用对应的消息处理方法
//        switch (pictureEditMessageTypeEnum) {
//            case ENTER_EDIT:
//                handleEnterEditMessage(pictureEditRequestMessage, session, user, pictureId);
//                break;
//            case EDIT_ACTION:
//                handleEditActionMessage(pictureEditRequestMessage, session, user, pictureId);
//                break;
//            case EXIT_EDIT:
//                handleExitEditMessage(pictureEditRequestMessage, session, user, pictureId);
//                break;
//            default:
//                PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
//                pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.ERROR.getValue());
//                pictureEditResponseMessage.setMessage("消息类型错误");
//                pictureEditResponseMessage.setUser(userService.getUserVO(user));
//                session.sendMessage(new TextMessage(JSONUtil.toJsonStr(pictureEditResponseMessage)));
//        }
    }

    public void handleEnterEditMessage(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session, User user, Long pictureId) throws Exception {
        // Redis 原子锁：没有用户正在编辑该图片，才能进入编辑
        if (pictureEditStateService.acquireLock(pictureId, user.getId())) {
            PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
            pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.ENTER_EDIT.getValue());
            String message = String.format("%s开始编辑图片", user.getUserName());
            pictureEditResponseMessage.setMessage(message);
            pictureEditResponseMessage.setUser(userService.getUserVO(user));
            broadcastToPicture(pictureId, pictureEditResponseMessage);
        }
    }


    public void handleEditActionMessage(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session, User user, Long pictureId) throws Exception {
        Long editingUserId = pictureEditStateService.getLockOwner(pictureId);
        String editAction = pictureEditRequestMessage.getEditAction();
        PictureEditActionEnum actionEnum = PictureEditActionEnum.getEnumByValue(editAction);
        if (actionEnum == null) {
            return;
        }
        // 确认是当前编辑者
        if (editingUserId != null && editingUserId.equals(user.getId())) {
            PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
            pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.EDIT_ACTION.getValue());
            String message = String.format("%s执行%s", user.getUserName(), actionEnum.getText());
            pictureEditResponseMessage.setMessage(message);
            pictureEditResponseMessage.setEditAction(editAction);
            pictureEditResponseMessage.setUser(userService.getUserVO(user));
            // 广播给除了当前客户端之外的其他用户，否则会造成重复编辑
            broadcastToPicture(pictureId, pictureEditResponseMessage, session);
        }
    }


    public void handleExitEditMessage(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session, User user, Long pictureId) throws Exception {
        Long editingUserId = pictureEditStateService.getLockOwner(pictureId);
        if (editingUserId != null && editingUserId.equals(user.getId())) {
            // 移除 Redis 编辑锁
            pictureEditStateService.releaseLock(pictureId);
            // 构造响应，发送退出编辑的消息通知
            PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
            pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.EXIT_EDIT.getValue());
            String message = String.format("%s退出编辑图片", user.getUserName());
            pictureEditResponseMessage.setMessage(message);
            pictureEditResponseMessage.setUser(userService.getUserVO(user));
            broadcastToPicture(pictureId, pictureEditResponseMessage);
        }
    }



    @Override
    public void afterConnectionClosed(WebSocketSession session, @NonNull CloseStatus status) throws Exception {
        Map<String, Object> attributes = session.getAttributes();
        Long pictureId = (Long) attributes.get("pictureId");
        User user = (User) attributes.get("user");
        // 移除当前用户的编辑状态
        handleExitEditMessage(null, session, user, pictureId);

        // 删除会话
        Set<WebSocketSession> sessionSet = pictureSessions.get(pictureId);
        if (sessionSet != null) {
            sessionSet.remove(session);

            //最后一个人关闭连接的时候整个picture对应的session也会被删掉
            if (sessionSet.isEmpty()) {
                pictureSessions.remove(pictureId);
            }
        }

        // 响应
        PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
        pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.INFO.getValue());
        String message = String.format("%s离开编辑", user.getUserName());
        pictureEditResponseMessage.setMessage(message);
        pictureEditResponseMessage.setUser(userService.getUserVO(user));
        broadcastToPicture(pictureId, pictureEditResponseMessage);
    }


    /**
     * 广播消息，不包括自己
     * @param pictureId
     * @param pictureEditResponseMessage
     */
    private void broadcastToPicture(Long pictureId, PictureEditResponseMessage pictureEditResponseMessage,WebSocketSession excludeSession) throws Exception {

        Set<WebSocketSession> sessionSet = pictureSessions.get(pictureId);
        if(CollUtil.isNotEmpty(sessionSet)){

            String str = toJsonString(pictureEditResponseMessage);
            TextMessage textMessage = new TextMessage(str);

            for (WebSocketSession webSocketSession : sessionSet) {
                if(webSocketSession.isOpen()){

                    if(excludeSession != null && excludeSession.equals(webSocketSession)){
                        continue;
                    }
                    // ★ 同步发送：防止 TEXT_PARTIAL_WRITING 并发写异常
                    synchronized (webSocketSession) {
                        try {
                            webSocketSession.sendMessage(textMessage);
                        } catch (Exception e) {
                            log.error("Failed to send message to session {}: {}", webSocketSession.getId(), e.getMessage());
                        }
                    }
                }
            }

            // ★ 快照已在 WorkHandler 中更新，此处不再写消息历史
        }


    }

    /**
     * 广播给该图片的所有用户（公开方法，供 WorkHandler 调用）
     */
    public void broadcastToPictureExclude(Long pictureId, PictureEditResponseMessage pictureEditResponseMessage,
                                           WebSocketSession excludeSession) throws Exception {
        broadcastToPicture(pictureId, pictureEditResponseMessage, excludeSession);
    }

    /**
     * 广播给该图片的所有用户（不排除任何人）
     */
    private void broadcastToPicture(Long pictureId, PictureEditResponseMessage pictureEditResponseMessage) throws Exception {
        broadcastToPicture(pictureId, pictureEditResponseMessage, null);
    }

    /**
     * 处理保存图片消息（阶段3实现）
     */
    public void handleSaveImageMessage(PictureEditRequestMessage req, WebSocketSession session,
                                        User user, Long pictureId) throws Exception {
        String pictureUrl = null;
        if (req.getParams() != null) {
            Object urlObj = req.getParams().get("pictureUrl");
            if (urlObj != null) pictureUrl = urlObj.toString();
        }

        PictureEditResponseMessage res = new PictureEditResponseMessage();
        res.setType(PictureEditMessageTypeEnum.SAVE_COMPLETE.getValue());
        String userName = user.getUserName();
        res.setMessage(String.format("%s 保存了图片", userName != null ? userName : "用户"));
        res.setUser(userService.getUserVO(user));
        res.setPictureUrl(pictureUrl);
        res.setTimestamp(System.currentTimeMillis());
        broadcastToPicture(pictureId, res);

        // ★ SAVE_COMPLETE 后清空快照（新图片重新开始）
        pictureEditStateService.deleteSnapshot(pictureId);
    }
}
