package cn.lhllhl.pixelisle.aop;

import cn.lhllhl.pixelisle.annotation.AuthCheck;
import cn.lhllhl.pixelisle.exception.BusinessException;
import cn.lhllhl.pixelisle.exception.ErrorCode;
import cn.lhllhl.pixelisle.model.entity.User;
import cn.lhllhl.pixelisle.model.enums.UserRoleEnum;
import cn.lhllhl.pixelisle.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

@Component
@Aspect
@Slf4j
public class AuthInterceptor {


    @Autowired
    private UserService userService;


    /**
     * 鉴权注解（最少用户级）
     * @param joinPoint
     * @param authCheck
     * @return
     * @throws Throwable
     */
    @Around("@annotation(authCheck)")
    public Object authInterceptor(ProceedingJoinPoint joinPoint , AuthCheck authCheck) throws Throwable {



        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();


        UserRoleEnum value = authCheck.value();


        if(value == null){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }


        User loginUser = userService.getLoginUser(request);

        String userrole = loginUser.getUserRole();

        if(userrole==null){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        UserRoleEnum enumByValue = UserRoleEnum.getEnumByValue(userrole);
        if(enumByValue==null){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        if(value==UserRoleEnum.ADMIN && enumByValue!=UserRoleEnum.ADMIN){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);

        }


        return joinPoint.proceed();


    }




}
