package cn.lhllhl.pixelisle.model.dto.space;

import lombok.Data;

import java.io.Serializable;

/**
 * 创建空间请求（请求只有管理员才能创建更高的级别的空间，用户付费创建更高级别的空间在其他的接口）
 */
@Data
public class SpaceAddRequest implements Serializable {

    /**
     * 空间名称
     */
    private String spaceName;

    /**
     * 空间级别: 0-普通版 1-专业版 2-旗舰版
     */
    private Integer spaceLevel;

    /**
     * 0--私有空间  1--团队空间
     */
    private Integer spaceType;

    private static final long serialVersionUID = 1L;
}