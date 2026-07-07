package cn.lhllhl.pixelisle.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class CaffeConfig {

    @Bean("LOCAL_CACHE")
    Cache<String,String> getCache(){

        Cache<String,String> LOCAL_CACHE= Caffeine.newBuilder().
                initialCapacity(1024).maximumSize(10000L).
                expireAfterWrite(Duration.ofMinutes(5)).build();

        return LOCAL_CACHE;
    }
}
