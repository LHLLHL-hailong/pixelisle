package cn.lhllhl.pixelisle.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 用户角色枚举
 */
@Getter
public enum UserRoleEnum {


USER("用户","user"),
    VIP("会员","vip"),
    ADMIN("管理员","admin");



    private   final String test;
    private final String value;


    UserRoleEnum(String test, String value) {
        this.test = test;
        this.value = value;
    }


    public static UserRoleEnum getEnumByValue(String value) {

        if(ObjUtil.isEmpty(value)){
            return null;
        }

        for (UserRoleEnum anEnum : UserRoleEnum.values()) {
            if(anEnum.value.equals(value)){
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
