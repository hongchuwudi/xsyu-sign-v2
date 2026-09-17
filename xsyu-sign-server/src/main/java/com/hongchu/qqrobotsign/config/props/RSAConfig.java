package com.hongchu.qqrobotsign.config.props;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * RSA配置类
 * 从配置文件中读取RSA密钥
 */
@Data
@Component
@ConfigurationProperties(prefix = "hc.rsa")
public class RSAConfig {
    /**
     * RSA私钥（Base64编码，仅后端使用）
     */
    private String privateKey;

    /**
     * RSA公钥（Base64编码，给前端使用）
     */
    private String publicKey;
}
