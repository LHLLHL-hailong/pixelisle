package cn.lhllhl.pixelisle.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * RedisTemplate 自定义配置类
 * 核心：替换默认序列化方式为 String + JSON，提升可读性和兼容性
 */
@Configuration
public class newRedisConfig {

    /**
     * 自定义 RedisTemplate
     * @param lettuceConnectionFactory Spring 自动注入的 Lettuce 连接工厂（默认）
     * @return 配置好的 RedisTemplate 实例
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(LettuceConnectionFactory lettuceConnectionFactory) {
        // 1. 创建 RedisTemplate 实例
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();

        // 2. 设置 Redis 连接工厂
        redisTemplate.setConnectionFactory(lettuceConnectionFactory);

        // 3. 配置 Key 序列化（字符串，保证 Redis 中 Key 可读）
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        redisTemplate.setKeySerializer(stringRedisSerializer);
        redisTemplate.setHashKeySerializer(stringRedisSerializer); // Hash 类型的 Key 也用字符串

        // 4. 配置 Value 序列化（修复：手动配置ObjectMapper，支持日期类型）
        // 4.1 手动创建ObjectMapper，注册JavaTimeModule（处理LocalDateTime）
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule()); // 核心：添加Java 8日期支持
        // 4.2 关闭“类型ID嵌入”（解决Type id handling异常）
        objectMapper.activateDefaultTyping(
                objectMapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL // 完全关闭类型ID嵌入
        );

        // 4.3 用自定义的ObjectMapper创建GenericJackson2JsonRedisSerializer
        GenericJackson2JsonRedisSerializer jsonRedisSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);
        redisTemplate.setValueSerializer(jsonRedisSerializer);
        redisTemplate.setHashValueSerializer(jsonRedisSerializer); // Hash 类型的 Value 也用 JSON

        // 5. 初始化配置，使序列化设置生效
        redisTemplate.afterPropertiesSet();

        return redisTemplate;
    }
}