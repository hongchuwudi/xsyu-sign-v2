package com.hongchu.qqrobotsign.storage;

import java.io.InputStream;

public interface ObjectStorageService {
    void put(String objectKey, InputStream inputStream, long size, String contentType);
    void delete(String objectKey);
}
