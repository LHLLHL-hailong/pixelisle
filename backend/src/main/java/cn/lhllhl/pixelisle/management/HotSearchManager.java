package cn.lhllhl.pixelisle.management;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 单机版热点搜索词检测+缓存管理器
 * 核心：滑动时间窗口统计频次 + Caffeine本地缓存
 */
@Component
public class HotSearchManager {
    // ========== 可配置参数（根据你的图库系统调整） ==========
    /** 滑动窗口大小，比如10秒内的访问频次 */
    private static final long WINDOW_SIZE_MS = 10 * 1000;
    /** 热点阈值：窗口内访问次数超过这个值就判定为热点词 */
    private static final long HOT_THRESHOLD = 5;
    /** 热点词缓存过期时间（避免缓存一直占用内存） */
    private static final long CACHE_EXPIRE_SECONDS = 60;

    // ========== 核心数据结构 ==========
    /** 存储每个搜索词的访问频次（滑动窗口） */
    private final ConcurrentHashMap<String, WindowCounter> counterMap = new ConcurrentHashMap<>();
//    /** 热点词本地缓存（Caffeine性能远高于HashMap） */
//    private final Cache<String, String> hotKeyCache = Caffeine.newBuilder()
//            .expireAfterWrite(CACHE_EXPIRE_SECONDS, TimeUnit.SECONDS) // 缓存过期时间
//            .maximumSize(1000) // 缓存最大容量（避免OOM）
//            .build();

    // 单例模式（单机系统用单例足够）
    private static volatile HotSearchManager INSTANCE;
    private HotSearchManager() {}
    public static HotSearchManager getInstance() {
        if (INSTANCE == null) {
            synchronized (HotSearchManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new HotSearchManager();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * 核心方法：处理搜索请求，自动统计频次+检测热点+缓存
     * @param searchWord 搜索词
     * @return 搜索结果（热点词直接返回缓存，非热点返回真实数据）
     */
    public Boolean processSearch(String searchWord) {


        // 2. 非热点词：统计访问频次
        long currentCount = countAndGet(searchWord);

        // 3. 判断是否达到热点阈值
        if (currentCount >= HOT_THRESHOLD) {
            // 4. 判定为热点词：加载数据并缓存
            return true;
        }


        return false;

        // 5. 非热点词：直接返回真实数据
    }

    /**
     * 滑动窗口统计访问频次
     */
    private long countAndGet(String key) {
        long now = System.currentTimeMillis();
        // 初始化/获取该key的计数器
        WindowCounter counter = counterMap.computeIfAbsent(key, k -> new WindowCounter());
        
        // 加锁保证线程安全（单机场景轻量锁无性能问题）
        synchronized (counter) {
            // 清理过期的时间窗口（只保留最近WINDOW_SIZE_MS内的计数）
            Iterator<Map.Entry<Long, AtomicLong>> iterator = counter.windowCounts.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Long, AtomicLong> entry = iterator.next();
                if (now - entry.getKey() > WINDOW_SIZE_MS) {
                    iterator.remove();
                }
            }

            // 当前时间戳作为窗口key（也可以按秒/100ms分桶，减少key数量）
            long windowKey = now;
            // 初始化当前窗口的计数器
            AtomicLong count = counter.windowCounts.computeIfAbsent(windowKey, k -> new AtomicLong(0));
            // 计数+1
            count.incrementAndGet();

            // 计算总频次：所有有效窗口的计数之和
            long total = 0;
            for (AtomicLong c : counter.windowCounts.values()) {
                total += c.get();
            }

            // 可选：如果长时间无访问，清理计数器（避免内存泄漏）
            if (total == 0) {
                counterMap.remove(key);
            }

            return total;
        }
    }

//    /**
//     * 获取所有热点词（用于展示/监控）
//     */
//    public Set<String> getHotKeys() {
//        return new HashSet<>(hotKeyCache.asMap().keySet());
//    }
//

    /**
     * 滑动窗口计数器
     */
    @Data
    private static class WindowCounter {
        /** key: 时间戳，value: 该时间戳的访问次数 */
        private final Map<Long, AtomicLong> windowCounts = new HashMap<>();
    }
}