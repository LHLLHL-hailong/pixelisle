package cn.lhllhl.pixelisle;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;


@EnableCaching
@EnableAsync
@SpringBootApplication
@MapperScan("cn.lhllhl.pixelisle.mapper")
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class PixelIsleApplication {



    public static void main(String[] args) {
        SpringApplication.run(PixelIsleApplication.class, args);
    }

}
