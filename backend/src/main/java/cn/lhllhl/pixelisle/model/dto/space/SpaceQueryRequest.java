package cn.lhllhl.pixelisle.model.dto.space;

import cn.lhllhl.pixelisle.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/*
 * 查询空间请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class SpaceQueryRequest extends PageRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 用户id
     */
    private Long userId;

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