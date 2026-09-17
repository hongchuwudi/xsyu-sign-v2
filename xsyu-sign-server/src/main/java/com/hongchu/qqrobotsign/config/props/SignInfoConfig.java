package com.hongchu.qqrobotsign.config.props;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "hc.sign-infos")
public class SignInfoConfig {
    private Integer inArea;
    private String areaJson;
    private Double latitude;
    private Double longitude;
}