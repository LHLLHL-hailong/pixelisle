package cn.lhllhl.pixelisle.management.webscoket.disruptor;

import cn.hutool.core.thread.ThreadFactoryBuilder;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.ExceptionHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

/**
 * 图片编辑事件 Disruptor 配置
 */
@Slf4j
@Configuration
public class PictureEditEventDisruptorConfig {

    @Resource
    private PictureEditEventWorkHandler pictureEditEventWorkHandler;

    @Bean("pictureEditEventDisruptor")
    public Disruptor<PictureEditEvent> messageModelRingBuffer() {
        // 定义 ringBuffer 的大小
        int bufferSize = 1024 * 256;
        // 创建 disruptor
        Disruptor<PictureEditEvent> disruptor = new Disruptor<>(
                PictureEditEvent::new,
                bufferSize,
                ThreadFactoryBuilder.create()
                        .setNamePrefix("pictureEditEventDisruptor")
                        .build()
        );
        // 设置消费者
        disruptor.handleEventsWithWorkerPool(pictureEditEventWorkHandler);
        // ★ 设置异常处理器：防止单条消息异常导致消费者线程停止
        disruptor.setDefaultExceptionHandler(new ExceptionHandler<PictureEditEvent>() {
            @Override
            public void handleEventException(Throwable ex, long sequence, PictureEditEvent event) {
                log.error("Disruptor event exception at sequence {}: {}", sequence, ex.getMessage(), ex);
            }

            @Override
            public void handleOnStartException(Throwable ex) {
                log.error("Disruptor onStart exception: {}", ex.getMessage(), ex);
            }

            @Override
            public void handleOnShutdownException(Throwable ex) {
                log.error("Disruptor onShutdown exception: {}", ex.getMessage(), ex);
            }
        });
        // 启动 disruptor
        disruptor.start();

        return disruptor;
    }
}