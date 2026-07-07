package cn.lhllhl.pixelisle.model.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 鍥剧墖
 * @TableName picture
 */
@TableName(value ="picture")
@Data
public class Picture implements Serializable {
    /**
     * id
     */
    @TableId(type = IdType.ASSIGN_ID)
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
    private String tags;

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
    @TableLogic
    private Integer isDelete;

    /**
     * 审核状态  0--待审核  1--通过  2--拒绝
     */
    private Integer reviewStatus;

    /**
     * 审核信息
     */
    private String reviewMessage;

    /**
     * 审核人ID
     */
    private Long reviewerId;

    /**
     *审核时间
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

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}