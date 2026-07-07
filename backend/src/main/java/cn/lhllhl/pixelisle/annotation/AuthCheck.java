package cn.lhllhl.pixelisle.annotation;


import cn.lhllhl.pixelisle.model.enums.UserRoleEnum;

import java.lang.annotation.ElementType;

@java.lang.annotation.Target({ElementType.METHOD})
@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface AuthCheck {

    UserRoleEnum value();
}
