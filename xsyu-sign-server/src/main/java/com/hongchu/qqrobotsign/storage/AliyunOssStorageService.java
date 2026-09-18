package com.hongchu.qqrobotsign.storage;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ObjectMetadata;
import com.hongchu.qqrobotsign.config.props.OssStorageProperties;
import com.hongchu.qqrobotsign.exception.BusinessException;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.io.InputStream;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "xsyu.storage", name = "type", havingValue = "aliyun-oss", matchIfMissing = true)
public class AliyunOssStorageService implements ObjectStorageService {
    private final OssStorageProperties properties;
    private volatile OSS client;

    private OSS client() {
        if (properties.getAccessKeyId() == null || properties.getAccessKeyId().isBlank()
                || properties.getAccessKeySecret() == null || properties.getAccessKeySecret().isBlank()) {
            throw new BusinessException("OSS 未配置 AccessKey，请设置 ALIYUN_OSS_ACCESS_KEY_ID 和 ALIYUN_OSS_ACCESS_KEY_SECRET");
        }
        OSS current = client;
        if (current == null) {
            synchronized (this) {
                current = client;
                if (current == null) {
                    current = new OSSClientBuilder().build(properties.getEndpoint(), properties.getAccessKeyId(), properties.getAccessKeySecret());
                    client = current;
                }
            }
        }
        return current;
    }

    @Override
    public void put(String objectKey, InputStream inputStream, long size, String contentType) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(size);
        metadata.setContentType(contentType);
        try {
            client().putObject(properties.getBucket(), objectKey, inputStream, metadata);
        } catch (OSSException | ClientException e) {
            throw new BusinessException("图片上传到 OSS 失败，请稍后重试");
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            client().deleteObject(properties.getBucket(), objectKey);
        } catch (OSSException | ClientException e) {
            throw new BusinessException("删除 OSS 图片失败");
        }
    }

    @PreDestroy
    public void close() {
        if (client != null) client.shutdown();
    }
}
