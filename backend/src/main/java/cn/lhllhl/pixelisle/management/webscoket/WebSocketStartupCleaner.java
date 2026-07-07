package cn.lhllhl.pixelisle.management.webscoket;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 服务启动时清理残留的编辑锁
 */
@Component
public class WebSocketStartupCleaner {

    @Resource
    private PictureEditStateService stateService;

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        // 所有 WebSocket 连接已断开，清空所有编辑锁
        stateService.clearAllLocks();
    }
}
