package com.hongchu.qqrobotsign.config.props;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;

@Data
@Component
@ConfigurationProperties(prefix = "xsyu.storage.oss")
public class OssStorageProperties {
    private String endpoint;
    private String bucket;
    private String publicBaseUrl;
    private String objectPrefix = "xsyu-sign/media";
    private String accessKeyId;
    private String accessKeySecret;
    private DataSize maxFileSize = DataSize.ofMegabytes(10);

    public long maxFileSizeBytes() {
        return maxFileSize.toBytes();
    }
}
