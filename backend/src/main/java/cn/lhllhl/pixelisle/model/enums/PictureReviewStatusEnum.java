package cn.lhllhl.pixelisle.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 用户角色枚举
 */
@Getter
public enum PictureReviewStatusEnum {


    REVIEWING("待审核", 0),
    PASS("通过", 1),
    REJECT("拒绝", 2);


    private final String test;
    private final int value;


    PictureReviewStatusEnum(String test, int value) {
        this.test = test;
        this.value = value;
    }


    public static PictureReviewStatusEnum getEnumByValue(Integer value) {

        if (ObjUtil.isEmpty(value)) {
            return null;
        }

        for (PictureReviewStatusEnum anEnum : PictureReviewStatusEnum.values()) {
            if (anEnum.value == value) {
                return anEnum;
            }
        }

        return null;


//
//
//        if(value==null){
//            return null;
//        }


    }


}
