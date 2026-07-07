package cn.lhllhl.pixelisle.config;


import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.region.Region;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties(prefix = "cos.client")
@Data
@Configuration
public class CosClientConfig {

    /**
     * 域名
     */
    private String host;

    /**
     * secretId
     */
    private String secretId;

    /**
     * 密钥（注意不要泄露）
     */
    private String secretKey;

    /**
     * 区域
     */
    private String region;

    /**
     * 桶名
     */
    private String bucket;



    @Bean
    public COSClient cosClient() {



        COSCredentials cred = new BasicCOSCredentials(secretId, secretKey);

        ClientConfig clientConfig = new ClientConfig(new Region( region));

        // 强制使用HTTPS协议（5.6.54版本后默认HTTPS，此配置可显式指定）
        clientConfig.setHttpProtocol(HttpProtocol.https);

        // 3. 生成COS客户端实例
        return new COSClient(cred, clientConfig);
    }
}
