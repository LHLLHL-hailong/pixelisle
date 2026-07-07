package cn.lhllhl.pixelisle.model.dto.space;

import lombok.Data;

import java.io.Serializable;

/**
 * 编辑空间请求（给用户用的，只能去更改空间的名称）
 */
@Data
public class SpaceEditRequest implements Serializable {

    /**
     * 空间 id
     */
    private Long id;

    /**
     * 空间名称
     */
    private String spaceName;

    private static final long serialVersionUID = 1L;
}