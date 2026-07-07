package cn.lhllhl.pixelisle.model.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 鐢ㄦ埛
 * @TableName user
 */
@TableName(value ="user")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User implements Serializable {
    /**
     * id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 璐﹀彿
     */
    private String userAccount;

    /**
     * 瀵嗙爜
     */
    private String userPassword;

    /**
     * 鐢ㄦ埛鏄电О
     */
    private String userName;

    /**
     * 鐢ㄦ埛澶村儚
     */
    private String userAvatar;

    /**
     * 鐢ㄦ埛绠?粙
     */
    private String userProfile;

    /**
     * 鐢ㄦ埛瑙掕壊锛歶ser/admin
     */
    private String userRole;

    /**
     * 缂栬緫鏃堕棿
     */
    private Date editTime;

    /**
     * 鍒涘缓鏃堕棿
     */
    private Date createTime;

    /**
     * 鏇存柊鏃堕棿
     */
    private Date updateTime;

    /**
     * 鏄?惁鍒犻櫎
     */
    @TableLogic(value = "0", delval = "NULL")
    private Integer isDelete;

    /**
     * 浼氬憳杩囨湡鏃堕棿
     */
    private Date vipExpireTime;

    /**
     * 浼氬憳鍏戞崲鐮
     */
    private String vipCode;

    /**
     * 浼氬憳缂栧彿
     */
    private Long vipNumber;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}