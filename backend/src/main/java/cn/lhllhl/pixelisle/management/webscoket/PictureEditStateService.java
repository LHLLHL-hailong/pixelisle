package cn.lhllhl.pixelisle.management.webscoket;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 图片编辑状态服务 —— Redis 持久化（阶段6：服务端画布快照）
 */
@Slf4j
@Service
public class PictureEditStateService {

    private static final String KEY_PREFIX_SNAP = "ws:snap:";
    private static final String KEY_PREFIX_LOCK = "ws:lock:";
    private static final long TTL_SECONDS = 3600; // 1小时

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    // ═══════════ 画布快照 ═══════════

    /**
     * 根据编辑消息更新服务端画布快照。同 key 覆盖写，天然替代压缩。
     */
    public void updateSnapshot(Long pictureId, String type, String editAction,
                               String targetId, String objectJSON,
                               Map<String, Object> cropParams) {
        String key = KEY_PREFIX_SNAP + pictureId;
        try {
            // 1. 取当前快照（不存在则初始化空快照）
            Object existing = redisTemplate.opsForValue().get(key);
            JSONObject snap = existing != null
                    ? JSONUtil.parseObj(existing.toString())
                    : newSnapshot();

            // 2. 根据消息类型更新对应字段
            switch (type) {
                case "OBJECT_ADDED":
                    addObject(snap, targetId, objectJSON);
                    break;
                case "OBJECT_MODIFIED":
                    if ("APPLY_FILTER".equals(editAction)) {
                        applyFilter(snap, targetId, objectJSON);
                    } else {
                        updateObject(snap, targetId, objectJSON);
                    }
                    break;
                case "OBJECT_REMOVED":
                    removeObject(snap, targetId);
                    break;
                case "CROP_CHANGE":
                    updateCrop(snap, cropParams);
                    break;
                default:
                    break; // ENTER_EDIT / EXIT_EDIT / PING 等不改变快照
            }

            // 3. 写回 Redis
            redisTemplate.opsForValue().set(key, snap.toString(), TTL_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("[Snapshot] updateSnapshot failed for picture {} type={} targetId={}",
                    pictureId, type, targetId, e);
        }
    }

    /**
     * 获取完整快照（新用户同步用）
     */
    public String getSnapshotForSync(Long pictureId) {
        try {
            String key = KEY_PREFIX_SNAP + pictureId;
            Object val = redisTemplate.opsForValue().get(key);
            return val != null ? val.toString() : null;
        } catch (Exception e) {
            log.error("[Snapshot] getSnapshotForSync failed for picture {}", pictureId, e);
            return null;
        }
    }

    /**
     * 删除快照（保存图片后清空，新图片重新开始）
     */
    public void deleteSnapshot(Long pictureId) {
        try {
            String key = KEY_PREFIX_SNAP + pictureId;
            redisTemplate.delete(key);
            log.info("[Snapshot] Deleted for picture {}", pictureId);
        } catch (Exception e) {
            log.error("[Snapshot] deleteSnapshot failed for picture {}", pictureId, e);
        }
    }

    // ── 快照字段更新 ──

    /** 新建空快照 */
    private JSONObject newSnapshot() {
        JSONObject snap = new JSONObject();
        snap.set("objects", new JSONArray());
        snap.set("crop", null);
        snap.set("filter", null);
        return snap;
    }

    /** OBJECT_ADDED：追加对象到数组末尾 */
    private void addObject(JSONObject snap, String targetId, String objectJSON) {
        if (targetId == null || objectJSON == null) return;
        JSONArray objects = snap.getJSONArray("objects");
        JSONObject entry = new JSONObject();
        entry.set("id", targetId);
        entry.set("data", JSONUtil.parseObj(objectJSON));
        objects.add(entry);
    }

    /** OBJECT_MODIFIED：同 id 替换 data */
    private void updateObject(JSONObject snap, String targetId, String objectJSON) {
        if (targetId == null || objectJSON == null) return;
        JSONArray objects = snap.getJSONArray("objects");
        for (int i = 0; i < objects.size(); i++) {
            JSONObject entry = objects.getJSONObject(i);
            if (targetId.equals(entry.getStr("id"))) {
                entry.set("data", JSONUtil.parseObj(objectJSON));
                return;
            }
        }
        // 没找到 → 当作新增（理论上不会走到这里，但兜底）
        log.warn("[Snapshot] OBJECT_MODIFIED for unknown targetId={}, adding as new", targetId);
        addObject(snap, targetId, objectJSON);
    }

    /** OBJECT_REMOVED：从数组中删除 */
    private void removeObject(JSONObject snap, String targetId) {
        if (targetId == null) return;
        JSONArray objects = snap.getJSONArray("objects");
        for (int i = 0; i < objects.size(); i++) {
            if (targetId.equals(objects.getJSONObject(i).getStr("id"))) {
                objects.remove(i);
                return;
            }
        }
    }

    /** 应用滤镜 */
    private void applyFilter(JSONObject snap, String targetId, String objectJSON) {
        if (objectJSON != null) {
            JSONObject data = JSONUtil.parseObj(objectJSON);
            snap.set("filter", data.getStr("filter"));
        }
    }

    /** 更新剪切框 */
    private void updateCrop(JSONObject snap, Map<String, Object> cropParams) {
        if (cropParams != null) {
            snap.set("crop", cropParams);
        }
    }

    // ═══════════ 编辑锁（不变） ═══════════

    public boolean acquireLock(Long pictureId, Long userId) {
        String key = KEY_PREFIX_LOCK + pictureId;
        Boolean ok = redisTemplate.opsForValue()
                .setIfAbsent(key, userId.toString(), TTL_SECONDS, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(ok);
    }

    public void releaseLock(Long pictureId) {
        String key = KEY_PREFIX_LOCK + pictureId;
        redisTemplate.delete(key);
    }

    public Long getLockOwner(Long pictureId) {
        String key = KEY_PREFIX_LOCK + pictureId;
        Object val = redisTemplate.opsForValue().get(key);
        if (val == null) return null;
        try {
            return Long.valueOf(val.toString());
        } catch (NumberFormatException e) {
            log.warn("Invalid lock value for picture {}: {}", pictureId, val);
            return null;
        }
    }

    public void heartbeat(Long pictureId) {
        String key = KEY_PREFIX_LOCK + pictureId;
        redisTemplate.expire(key, 300, TimeUnit.SECONDS);
    }

    public void clearAllLocks() {
        try {
            redisTemplate.execute((org.springframework.data.redis.core.RedisCallback<Void>) connection -> {
                try (Cursor<byte[]> cursor = connection.scan(
                        ScanOptions.scanOptions().match(KEY_PREFIX_LOCK + "*").count(200).build())) {
                    List<byte[]> batch = new ArrayList<>();
                    while (cursor.hasNext()) {
                        batch.add(cursor.next());
                        if (batch.size() >= 200) {
                            connection.del(batch.toArray(new byte[0][]));
                            batch.clear();
                        }
                    }
                    if (!batch.isEmpty()) {
                        connection.del(batch.toArray(new byte[0][]));
                    }
                }
                return null;
            });
            log.info("Cleared edit locks on startup (SCAN)");
        } catch (Exception e) {
            log.warn("Failed to clear locks on startup", e);
        }
    }
}
