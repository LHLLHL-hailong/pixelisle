package cn.lhllhl.pixelisle;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

@SpringBootTest
class PixelIsleApplicationTests {

    @Autowired
    RedisTemplate redisTemplate;

    @Autowired
    DataSource dataSource;

    @Test
    void contextLoads() {
    }

    @Test
    void DatasourceTest() throws SQLException {

        Connection connection = null;
        try {
            connection = dataSource.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            connection.close();
        }

    }

    @Test
    @DisplayName("redis连接测试")
    void RedisTemplateTest(){
        ValueOperations valueOperations = redisTemplate.opsForValue();

        valueOperations.set("k1","v1",1L, TimeUnit.HOURS);

        Object o = valueOperations.get("k1");

        Assertions.assertEquals("v1",o);

    }

}
