package cn.lhllhl.pixelisle.model.dto.space.analyze;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

/**
 * 通用空间分析请求
 */
@Data
public class SpaceAnalyzeRequest implements Serializable {

    /**
     * 空间 ID(只有下面两个都为false才生效)
     */
    private Long spaceId;

    /**
     * 是否查询公共图库
     */
    private boolean queryPublic;


    /**
     * 全空间分析
     */
    private boolean queryAll;

    private static final long serialVersionUID = 1L;


    //生成缓存key
    public String getKey(){
        return "SpaceAnalyzeRequest{" +
                "spaceId=" + spaceId +
                ", queryPublic=" + queryPublic +
                ", queryAll=" + queryAll +
                '}';
    }

}