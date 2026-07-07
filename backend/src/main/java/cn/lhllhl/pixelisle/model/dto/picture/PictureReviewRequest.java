package cn.lhllhl.pixelisle.model.dto.picture;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class PictureReviewRequest implements Serializable {

    private Long id;


    /**
     * 审核状态  0--待审核  1--通过  2--拒绝
     */
    private Integer reviewStatus;

    /**
     * 审核信息
     */
    private String reviewMessage;

//    /**
//     * 审核人ID
//     */
//    private Long reviewerId;   //自动填充
//
//    /**
//     *审核时间
//     */
//    private Date reviewTime;   //自动填充


}
