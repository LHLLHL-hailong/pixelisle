package cn.lhllhl.pixelisle.task;

import cn.lhllhl.pixelisle.mapper.PictureMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Slf4j
@Service
public class ClearTaskService {

    @Resource
    private PictureMapper pictureMapper;





    // 每天凌晨 2 点执行
   // @Scheduled(cron = "0 0 2 * * ?")
    public void clearDeletedPictures() {
//        log.info("===== 开始清理逻辑删除图片 =====");
//
//        int batchSize = 500; // 每批处理 500 条
//        long sleepMs = 200;  // 每批间隔 200ms
//        long currentMaxId = 0;
//        int totalDeleted = 0;
//
//        while (true) {
//            // 1. 分批查询：获取下一批需要删除的数据 (只查 id 和 url)
//            // SQL: SELECT id, url FROM picture WHERE id > #{currentMaxId} AND is_deleted = 1 ORDER BY id ASC LIMIT #{batchSize}
//            List<PictureScheduleDeleteBean> batchList = pictureMapper.selectPictureScheduleList(currentMaxId, batchSize);
//
//            if (CollUtil.isEmpty(batchList)) {
//                log.info("===== 清理完成，共清理 {} 条 =====", totalDeleted);
//                break;
//            }
//
//            // 2. 提取这一批的 URL 和 ID
//            List<String> urls = batchList.stream().map(PictureScheduleDeleteBean::getUrl).collect(Collectors.toList());
//            List<Long> ids = batchList.stream().map(PictureScheduleDeleteBean::getId).collect(Collectors.toList());
//            Long thisBatchMaxId = ids.get(ids.size() - 1);
//
//            try {
//                // 3. 【重要】先删除 OSS 上的图片 (批量删除)
//                //ossService.batchDelete(urls);
//                //todo:删除对象存储
//
//
//
//                // 4. 再删除数据库里的记录 (用 ID 范围删，性能最好)
//                // SQL: DELETE FROM picture WHERE id <= #{thisBatchMaxId} AND id > #{currentMaxId} AND is_deleted = 1
//                pictureMapper.deleteByRangeidAndisDeleted(currentMaxId, thisBatchMaxId);
//
//                totalDeleted += batchList.size();
//                log.info("已清理批次: ID范围 ({}, {}], 本次数量: {}", currentMaxId, thisBatchMaxId, batchList.size());
//
//                // 5. 挪动游标
//                currentMaxId = thisBatchMaxId;
//
//                // 6. 【关键】休眠一下，防止把数据库压垮
//                Thread.sleep(sleepMs);
//
//            } catch (Exception e) {
//                log.error("清理批次出错 ID范围: ({}, {}]", currentMaxId, thisBatchMaxId, e);
//                // 这里可以选择 break 或者 continue，建议 break 并告警，人工介入
//                break;
//            }
      //  }
    }
}