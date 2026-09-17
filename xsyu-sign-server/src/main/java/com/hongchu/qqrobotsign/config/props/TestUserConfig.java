package com.hongchu.qqrobotsign.config.props;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "hc.test-user")
public class TestUserConfig {
    private String username;
    private String password;
}