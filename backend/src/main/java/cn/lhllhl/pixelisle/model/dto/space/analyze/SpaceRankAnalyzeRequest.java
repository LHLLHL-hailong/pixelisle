package cn.lhllhl.pixelisle.model.dto.space.analyze;

import lombok.Data;
import java.io.Serializable;

@Data
public class SpaceRankAnalyzeRequest implements Serializable {

    /**
     * 排名前 N 的空间
     * 默认提取前10的空间
     */
    private Integer topN = 10;

    private static final long serialVersionUID = 1L;
}