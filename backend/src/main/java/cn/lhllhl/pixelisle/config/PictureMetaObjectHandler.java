package cn.lhllhl.pixelisle.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

/**
 * Picture 实体自动填充处理器
 * 确保 INSERT 时 spaceId 始终有确定值，ShardingSphere 能精确路由
 */
@Component
@Slf4j
public class PictureMetaObjectHandler implements MetaObjectHandler {

    /**
     * INSERT 时自动填充：如果 spaceId 为 null，设为 0（表示公共图库）
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        if (!metaObject.hasGetter("spaceId")) {
            return;
        }
        Object spaceId = this.getFieldValByName("spaceId", metaObject);
        if (spaceId == null) {
            this.setFieldValByName("spaceId", 0L, metaObject);
            log.debug("PictureMetaObjectHandler: 自动填充 spaceId = 0 (公共图库)");
        }
    }

    /**
     * UPDATE 时不做填充，spaceId 应保持原值
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        // 更新操作不需要修改 spaceId
    }
}
