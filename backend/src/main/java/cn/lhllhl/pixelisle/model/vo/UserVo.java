package cn.lhllhl.pixelisle.model.vo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户视图(脱敏)
 * @TableName user
 */
@TableName(value ="user")
@Data
@Builder
public class UserVo implements Serializable {
    private static final long serialVersionUID = -2178326549369602423L;
    /**
     * id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    public UserVo() {
    }

    public UserVo(Long id, String userAccount, String userName, String userAvatar, String userProfile, String userRole, Date createTime, Date vipExpireTime, String vipCode, Long vipNumber) {
        this.id = id;
        this.userAccount = userAccount;
        this.userName = userName;
        this.userAvatar = userAvatar;
        this.userProfile = userProfile;
        this.userRole = userRole;
        this.createTime = createTime;
        this.vipExpireTime = vipExpireTime;
        this.vipCode = vipCode;
        this.vipNumber = vipNumber;
    }

    /**
     * 璐﹀彿
     */
    private String userAccount;


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
     * 鍒涘缓鏃堕棿
     */
    private Date createTime;




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

}