package cn.lhllhl.pixelisle.model.vo;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.lhllhl.pixelisle.model.entity.Picture;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 鍥剧墖
 *
 * @TableName picture
 */
@TableName(value = "picture")
@Data
public class PictureVo implements Serializable {
    /**
     * id
     */
    private Long id;

    /**
     * 鍥剧墖 url
     */
    private String url;

    /**
     * 鍥剧墖鍚嶇О
     */
    private String name;

    /**
     * 绠?粙
     */
    private String introduction;

    /**
     * 鍒嗙被
     */
    private String category;

    /**
     * 鏍囩?锛圝SON 鏁扮粍锛
     */
    private List<String> tags;

    /**
     * 鍥剧墖浣撶Н
     */
    private Long picSize;

    /**
     * 鍥剧墖瀹藉害
     */
    private Integer picWidth;

    /**
     * 鍥剧墖楂樺害
     */
    private Integer picHeight;

    /**
     * 鍥剧墖瀹介珮姣斾緥
     */
    private Double picScale;

    /**
     * 鍥剧墖鏍煎紡
     */
    private String picFormat;

    /**
     * 鍒涘缓鐢ㄦ埛 id
     */
    private Long userId;

    /**
     * 鍒涘缓鏃堕棿
     */
    private Date createTime;

    /**
     * 缂栬緫鏃堕棿
     */
    private Date editTime;

    /**
     * 鏇存柊鏃堕棿
     */
    private Date updateTime;

    /**
     * 鏄?惁鍒犻櫎
     */
    private Integer isDelete;

    /**
     * 瀹℃牳鐘舵?锛?-寰呭?鏍? 1-閫氳繃; 2-鎷掔粷
     */
    private Integer reviewStatus;

    /**
     * 瀹℃牳淇℃伅
     */
    private String reviewMessage;

    /**
     * 瀹℃牳浜?ID
     */
    private Long reviewerId;

    /**
     * 瀹℃牳鏃堕棿
     */
    private Date reviewTime;

    /**
     * 缂╃暐鍥?url
     */
    private String thumbnailUrl;

    /**
     * 绌洪棿 id锛堜负绌鸿〃绀哄叕鍏辩┖闂达級
     */
    private Long spaceId;

    /**
     * 鍥剧墖涓昏壊璋
     */
    private String picColor;


    private UserVo userVo;

    /**
     * 权限列表
     */
    private List<String> permissionList = new ArrayList<>();

    public static PictureVo pictureToPictureVo(Picture picture) {

        if (picture == null) {
            return null;
        }


        PictureVo pictureVo = new PictureVo();
        BeanUtils.copyProperties(picture, pictureVo);

        List<String> list = JSONUtil.toList(picture.getTags(), String.class);

        pictureVo.setTags(list);
        return pictureVo;

    }


   public static Picture picatureVoToPicture(PictureVo pictureVo) {
        if (pictureVo == null) {
            return null;
        }

        Picture picture = new Picture();

        BeanUtils.copyProperties(pictureVo, picture);

        List<String> tags1 = pictureVo.getTags();

        JSONObject jsonObject = JSONUtil.parseObj(pictureVo.getTags());


        String string = jsonObject.toString();

        picture.setTags(string);


        return picture;


    }


}