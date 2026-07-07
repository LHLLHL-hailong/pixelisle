package cn.lhllhl.pixelisle.model.dto.picture;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 图片的更新请求
 *
 * @TableName picture
 */
@TableName(value = "picture")
@Data
public class PictureUpdateRequest implements Serializable {
    private static final long serialVersionUID = 6877806519379628647L;
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