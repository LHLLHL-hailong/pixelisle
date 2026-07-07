package cn.lhllhl.pixelisle.model.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 绌洪棿
 * @TableName space
 */
@TableName(value ="space")
@Data
public class Space implements Serializable {
    /**
     * id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 绌洪棿鍚嶇О
     */
    private String spaceName;

    /**
     * 绌洪棿绾у埆锛?-鏅??鐗?1-涓撲笟鐗?2-鏃楄埌鐗
     */
    private Integer spaceLevel;

    /**
     * 绌洪棿鍥剧墖鐨勬渶澶ф?澶у皬
     */
    private Long maxSize;

    /**
     * 绌洪棿鍥剧墖鐨勬渶澶ф暟閲
     */
    private Long maxCount;

    /**
     * 褰撳墠绌洪棿涓嬪浘鐗囩殑鎬诲ぇ灏
     */
    private Long totalSize;

    /**
     * 褰撳墠绌洪棿涓嬬殑鍥剧墖鏁伴噺
     */
    private Long totalCount;

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
     * 绌洪棿绫诲瀷锛?-绉佹湁 1-鍥㈤槦
     */
    private Integer spaceType;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}