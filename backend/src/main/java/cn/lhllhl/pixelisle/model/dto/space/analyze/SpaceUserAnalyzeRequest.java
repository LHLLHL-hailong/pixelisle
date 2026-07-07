package cn.lhllhl.pixelisle.model.dto.space.analyze;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 空间用户上传行为分析请求
 * 可以指定某个用户，也可以不指定
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ToString
public class SpaceUserAnalyzeRequest extends SpaceAnalyzeRequest {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 时间维度: day / week / month
     */
    private String timeDimension;

    @Override
    public String getKey() {
        return this.toString()+super.getKey();
    }
}