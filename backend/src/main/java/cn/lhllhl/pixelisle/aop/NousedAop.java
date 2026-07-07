package cn.lhllhl.pixelisle.aop;



import cn.lhllhl.pixelisle.annotation.NoUsed;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class NousedAop {


    @Around("@annotation(authCheck)")
    public Object authInterceptor(ProceedingJoinPoint joinPoint , NoUsed authCheck) throws Throwable {

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();

        String name = signature.getName();

        log.error("方法意外触发 {} message {}",name,authCheck.value() );

        throw new RuntimeException("noUsed 方法被触发！！！");

    }
}
