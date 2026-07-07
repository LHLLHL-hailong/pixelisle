package cn.lhllhl.pixelisle.model.dto.picture;

import lombok.Data;

@Data
public class PictureUploadRequest {


    /**
     * 图片的id值
     */
    private Long id;


    /**
     * 文件的地址
     */
    private String fileUrl;




    /**
     * 图片名称的前缀
     */
    private String namePrefix;

    /**
     * 空间id
     */
    private Long spaceId;

    /**
     * 图片主色调
     */
    private String picColor;


}
