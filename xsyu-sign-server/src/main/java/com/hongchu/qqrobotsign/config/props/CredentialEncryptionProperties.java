package com.hongchu.qqrobotsign.config.props;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Data
@Component
@ConfigurationProperties(prefix = "xsyu.security.credential-encryption")
public class CredentialEncryptionProperties {

    private String masterKey;

    @PostConstruct
    public void validate() {
        if (!StringUtils.hasText(masterKey) || masterKey.length() < 32) {
            throw new IllegalStateException(
                    "学校密码加密主密钥未配置或长度不足，请设置至少 32 个字符的 XSYU_STUDENT_PASSWORD_MASTER_KEY");
        }
    }
}
