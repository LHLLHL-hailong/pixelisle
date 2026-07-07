package cn.lhllhl.pixelisle.model.dto.picture;

import lombok.Data;

/**
 * 图片上传的结果
 */
@Data
public class UploadPictureResult {


    /**
     * 图片地址
     */
    private String url;


    /**
     * 图片名称
     */
    private String picName;


    /**
     * 文件体积
     */
    private Long picSize;


    /**
     * 图片宽度
     */
    private int picWidth;


    /**
     * 图片高度
     */
    private int picHeight;


    /**
     * 图片的宽高比
     */
    private Double picScale;


    /**
     * 图片的格式
     */
    private String picFormat;


    /**
     * 缩略图url
     */
    private String thumbnailUrl;


    /**
     * 图片主要色调
     */
    private String picColor;


}
