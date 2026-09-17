package com.hongchu.qqrobotsign.config.props;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "hc.urls")
public class UrlConfig {
    private String baseUrl;
    private String uriPri;
    private String uriGetAllSign;
    private String uriGetOneSign;
    private String uriSign;
    
    public String getFullGetAllSignUrl() {
        return baseUrl + uriPri + uriGetAllSign;
    }
    
    public String getFullGetOneSignUrl() {
        return baseUrl + uriPri + uriGetOneSign;
    }
    
    public String getFullSignUrl() {
        return baseUrl + uriPri + uriSign;
    }
}