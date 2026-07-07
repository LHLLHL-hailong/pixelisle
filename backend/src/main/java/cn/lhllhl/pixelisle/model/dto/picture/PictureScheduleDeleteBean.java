package cn.lhllhl.pixelisle.model.dto.picture;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 鍥剧墖
 * @TableName picture
 */
@TableName(value ="picture")
@Data
public class PictureScheduleDeleteBean implements Serializable {
    /**
     * id
     */

    private Long id;

    /**
     * 鍥剧墖 url
     */
    private String url;


    /**
     * 缂栬緫鏃堕棿
     */
    private Date editTime;


}