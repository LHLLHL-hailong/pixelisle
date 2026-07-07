package cn.lhllhl.pixelisle.model.dto.picture;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 图片的编辑请求（用户）
 *
 * @TableName picture
 */
@TableName(value = "picture")
@Data
public class PictureEditRequest implements Serializable {

    private static final long serialVersionUID = 6043821223804008557L;
    /**
     * id
     */
    private Long id;

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


}