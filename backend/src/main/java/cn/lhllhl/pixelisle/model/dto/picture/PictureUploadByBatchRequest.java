package cn.lhllhl.pixelisle.model.dto.picture;

import lombok.Data;


/**
 * 批量导入图片的请求
 */
@Data
public class PictureUploadByBatchRequest {


    /**
     * 搜索词
     */
    private String searchText;


    /**
     * 抓取的数量（默认抓取十条）
     */
    private Integer count = 10;


    /**
     * 图片名称的前缀
     */
    private String namePrefix;

    /**
     * 空间 id（为空则上传到公共图库）
     */
    private Long spaceId;

}
