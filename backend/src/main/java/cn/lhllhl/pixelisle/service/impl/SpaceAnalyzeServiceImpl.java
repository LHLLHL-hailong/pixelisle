package cn.lhllhl.pixelisle.service.impl;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import cn.lhllhl.pixelisle.exception.BusinessException;
import cn.lhllhl.pixelisle.exception.ErrorCode;
import cn.lhllhl.pixelisle.exception.ThrowUtils;
import cn.lhllhl.pixelisle.model.dto.space.analyze.*;
import cn.lhllhl.pixelisle.model.entity.Picture;
import cn.lhllhl.pixelisle.model.entity.Space;
import cn.lhllhl.pixelisle.model.entity.User;
import cn.lhllhl.pixelisle.model.vo.space.analyze.*;
import cn.lhllhl.pixelisle.service.PictureService;
import cn.lhllhl.pixelisle.service.SpaceAnalyzeService;
import cn.lhllhl.pixelisle.service.SpaceService;
import cn.lhllhl.pixelisle.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SpaceAnalyzeServiceImpl implements SpaceAnalyzeService {

    @Autowired
    private UserService userService;

    @Autowired
    private SpaceService spaceService;

    @Autowired
    private PictureService pictureService;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    @Qualifier("LOCAL_CACHE")
    private Cache<String, String> LOCAL_CACHE;

    // ═══════════ 缓存 key 构建 ═══════════

    private String buildCacheKey(String type, SpaceAnalyzeRequest req) {
        String scope;
        if (req.isQueryAll()) {
            scope = "all";
        } else if (req.isQueryPublic()) {
            scope = "pub";
        } else {
            scope = "s" + req.getSpaceId();
        }
        return "ana:" + type + ":" + scope;
    }

    // ═══════════ 通用缓存存取 ═══════════

    @SuppressWarnings("unchecked")
    private <T> T getCached(String key, Class<T> clazz) {
        String local = LOCAL_CACHE.getIfPresent(key);
        if (local != null) {
            log.info("本地缓存命中 {}", key);
            return JSONUtil.toBean(local, clazz, false);
        }
        ValueOperations<String, Object> ops = redisTemplate.opsForValue();
        Object redisVal = ops.get(key);
        if (redisVal != null) {
            log.info("Redis 命中 {}，回写本地", key);
            LOCAL_CACHE.put(key, JSONUtil.toJsonStr(redisVal));
            return (T) redisVal;
        }
        return null;
    }

    private void putCache(String key, Object value) {
        long expireTime = 5L + RandomUtil.randomLong(0L, 5L);
        redisTemplate.opsForValue().set(key, value, expireTime, TimeUnit.MINUTES);
        LOCAL_CACHE.put(key, JSONUtil.toJsonStr(value));
        log.info("回写缓存 {}，TTL={}min", key, expireTime);
    }

    // ═══════════ 权限校验 ═══════════

    private void checkSpaceAnalyzeAuth(SpaceAnalyzeRequest spaceAnalyzeRequest, User loginUser) {
        boolean queryPublic = spaceAnalyzeRequest.isQueryPublic();
        boolean queryAll = spaceAnalyzeRequest.isQueryAll();
        if (queryAll || queryPublic) {
            ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR);
        } else {
            Long spaceId = spaceAnalyzeRequest.getSpaceId();
            ThrowUtils.throwIf(spaceId == null || spaceId == 0L, ErrorCode.PARAMS_ERROR);
            Space oldSpace = spaceService.getById(spaceId);
            spaceService.checkSpaceAuth(loginUser, oldSpace);
        }
    }

    private void fillAnalyzeQueryWrapper(SpaceAnalyzeRequest spaceAnalyzeRequest, QueryWrapper<Picture> queryWrapper) {
        Long spaceId = spaceAnalyzeRequest.getSpaceId();
        boolean queryPublic = spaceAnalyzeRequest.isQueryPublic();
        boolean queryAll = spaceAnalyzeRequest.isQueryAll();
        if (queryAll) {
            return;
        }
        if (queryPublic) {
            queryWrapper.and(w -> w.isNull("spaceId").or().eq("spaceId", 0L));
            return;
        }
        if (spaceId != null) {
            queryWrapper.eq("spaceId", spaceId);
            return;
        }
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "没有指定查询范围");
    }

    // ═══════════ 1. 空间使用分析 ═══════════

    @Override
    public SpaceUsageAnalyzeResponse getSpaceUsageAnalyze(SpaceUsageAnalyzeRequest req, User loginUser) {
        checkSpaceAnalyzeAuth(req, loginUser);
        String key = buildCacheKey("usage", req);

        SpaceUsageAnalyzeResponse cached = getCached(key, SpaceUsageAnalyzeResponse.class);
        if (cached != null) return cached;

        boolean queryPublic = req.isQueryPublic();
        boolean queryAll = req.isQueryAll();
        SpaceUsageAnalyzeResponse response = new SpaceUsageAnalyzeResponse();

        if (queryAll || queryPublic) {
            QueryWrapper<Picture> qw = new QueryWrapper<>();
            qw.select("picSize");
            fillAnalyzeQueryWrapper(req, qw);
            List<Object> list = pictureService.getBaseMapper().selectObjs(qw);
            response.setUsedSize(list.stream().mapToLong(a -> (long) a).sum());
            response.setUsedCount((long) list.size());
            response.setMaxSize(null);
            response.setSizeUsageRatio(null);
            response.setMaxCount(null);
            response.setCountUsageRatio(null);
        } else {
            Space space = spaceService.getById(req.getSpaceId());
            response.setUsedSize(space.getTotalSize());
            response.setMaxSize(space.getMaxSize());
            response.setSizeUsageRatio(NumberUtil.round(space.getTotalSize() * 100.0 / space.getMaxSize(), 2).doubleValue());
            response.setUsedCount(space.getTotalCount());
            response.setMaxCount(space.getMaxCount());
            response.setCountUsageRatio(NumberUtil.round(space.getTotalCount() * 100.0 / space.getMaxCount(), 2).doubleValue());
        }

        putCache(key, response);
        return response;
    }

    // ═══════════ 2. 分类分析 ═══════════

    @Override
    public List<SpaceCategoryAnalyzeResponse> getSpaceCategoryAnalyze(SpaceCategoryAnalyzeRequest req, User loginUser) {
        checkSpaceAnalyzeAuth(req, loginUser);
        String key = buildCacheKey("category", req);

        List cached = getCached(key, List.class);
        if (cached != null) return cached;

        QueryWrapper<Picture> qw = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(req, qw);
        qw.select("category", "count(*) as count", "sum(picSize) as totalSize").groupBy("category");

        List<SpaceCategoryAnalyzeResponse> result = pictureService.getBaseMapper().selectMaps(qw)
                .stream()
                .map(m -> {
                    String category = (String) m.get("category");
                    Long count = (Long) m.get("count");
                    Long totalSize = ((BigDecimal) m.get("totalSize")).longValue();
                    return new SpaceCategoryAnalyzeResponse(category, count, totalSize);
                })
                .collect(Collectors.toList());

        putCache(key, result);
        return result;
    }

    // ═══════════ 3. 标签分析 ═══════════

    @Override
    public List<SpaceTagAnalyzeResponse> getSpaceTagAnalyze(SpaceTagAnalyzeRequest req, User loginUser) {
        checkSpaceAnalyzeAuth(req, loginUser);
        String key = buildCacheKey("tag", req);

        List cached = getCached(key, List.class);
        if (cached != null) return cached;

        QueryWrapper<Picture> qw = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(req, qw);
        qw.select("tags");
        List<String> tagsJsonList = pictureService.getBaseMapper().selectObjs(qw)
                .stream().filter(ObjUtil::isNotNull).map(Object::toString).collect(Collectors.toList());

        Map<String, Long> tagCountMap = tagsJsonList.stream()
                .flatMap(tagsJson -> JSONUtil.toList(tagsJson, String.class).stream())
                .collect(Collectors.groupingBy(tag -> tag, Collectors.counting()));

        List<SpaceTagAnalyzeResponse> result = tagCountMap.entrySet().stream()
                .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
                .map(e -> new SpaceTagAnalyzeResponse(e.getKey(), e.getValue()))
                .collect(Collectors.toList());

        putCache(key, result);
        return result;
    }

    // ═══════════ 4. 大小分布分析 ═══════════

    @Override
    public List<SpaceSizeAnalyzeResponse> getSpaceSizeAnalyze(SpaceSizeAnalyzeRequest req, User loginUser) {
        checkSpaceAnalyzeAuth(req, loginUser);
        String key = buildCacheKey("size", req);

        List cached = getCached(key, List.class);
        if (cached != null) return cached;

        QueryWrapper<Picture> qw = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(req, qw);
        qw.select("picSize");
        List<Long> picSizeList = pictureService.getBaseMapper().selectObjs(qw)
                .stream().filter(ObjUtil::isNotNull).map(size -> (Long) size).collect(Collectors.toList());

        Map<String, Long> sizeRanges = new LinkedHashMap<>();
        sizeRanges.put("<100KB", picSizeList.stream().filter(s -> s < 100 * 1024).count());
        sizeRanges.put("100KB-500KB", picSizeList.stream().filter(s -> s >= 100 * 1024 && s < 500 * 1024).count());
        sizeRanges.put("500KB-1MB", picSizeList.stream().filter(s -> s >= 500 * 1024 && s < 1 * 1024 * 1024).count());
        sizeRanges.put(">1MB", picSizeList.stream().filter(s -> s >= 1 * 1024 * 1024).count());

        List<SpaceSizeAnalyzeResponse> result = sizeRanges.entrySet().stream()
                .map(e -> new SpaceSizeAnalyzeResponse(e.getKey(), e.getValue()))
                .collect(Collectors.toList());

        putCache(key, result);
        return result;
    }

    // ═══════════ 5. 用户上传分析 ═══════════

    @Override
    public List<SpaceUserAnalyzeResponse> getSpaceUserAnalyze(SpaceUserAnalyzeRequest req, User loginUser) {
        checkSpaceAnalyzeAuth(req, loginUser);
        String key = buildCacheKey("user", req) + ":d" + req.getTimeDimension() + ":u" + loginUser.getId();

        List cached = getCached(key, List.class);
        if (cached != null) return cached;

        QueryWrapper<Picture> qw = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(req, qw);
        Long userId = req.getUserId();
        qw.eq(ObjUtil.isNotNull(userId), "userId", loginUser.getId());

        switch (req.getTimeDimension()) {
            case "day":   qw.select("DATE_FORMAT(createTime, '%Y-%m-%d') as period", "count(*) as count"); break;
            case "week":  qw.select("DATE_FORMAT(createTime, '%Y-%v') as period", "count(*) as count"); break;
            case "month": qw.select("DATE_FORMAT(createTime, '%Y-%m') as period", "count(*) as count"); break;
            default: throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的时间维度");
        }
        qw.groupBy("period").orderByAsc("period");

        List<SpaceUserAnalyzeResponse> result = pictureService.getBaseMapper().selectMaps(qw)
                .stream()
                .map(m -> new SpaceUserAnalyzeResponse((String) m.get("period"), (Long) m.get("count")))
                .collect(Collectors.toList());

        putCache(key, result);
        return result;
    }

    // ═══════════ 6. 空间排行（管理员） ═══════════

    @Override
    public List<Space> getSpaceRankAnalyze(SpaceRankAnalyzeRequest req, User loginUser) {
        ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR);
        String key = "ana:rank:" + req.getTopN();

        List cached = getCached(key, List.class);
        if (cached != null) return cached;

        QueryWrapper<Space> qw = new QueryWrapper<>();
        qw.select("id", "spaceName", "userId", "totalSize")
                .orderByDesc("totalSize")
                .last("limit " + req.getTopN());

        List<Space> result = spaceService.list(qw);
        putCache(key, result);
        return result;
    }
}
