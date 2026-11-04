package com.music.storage;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Service
public class HybridStorageService implements StorageService {

    private final LocalStorageService localStorageService;
    private final S3StorageService s3StorageService;
    private final Set<String> s3Types;

    public HybridStorageService(
            @Value("${app.upload-dir}") String uploadDir,
            @Value("${app.upload-url-prefix}") String urlPrefix,
            @Value("${app.storage.s3.endpoint:}") String s3Endpoint,
            @Value("${app.storage.s3.access-key:}") String s3AccessKey,
            @Value("${app.storage.s3.secret-key:}") String s3SecretKey,
            @Value("${app.storage.s3.bucket-name:}") String s3BucketName,
            @Value("${app.storage.s3.region:us-east-1}") String s3Region,
            @Value("${app.storage.s3.url-prefix:}") String s3UrlPrefix,
            @Value("${app.storage.s3-types:audio,cover,lyric}") String s3TypesConfig
    ) {
        this.localStorageService = new LocalStorageService(uploadDir, urlPrefix);

        // 仅在 S3 配置了 endpoint 和 access-key 时才启用 S3 存储
        boolean s3Configured = s3Endpoint != null && !s3Endpoint.isBlank()
                && s3AccessKey != null && !s3AccessKey.isBlank();
        if (s3Configured) {
            this.s3StorageService = new S3StorageService(s3Endpoint, s3AccessKey, s3SecretKey, s3BucketName, s3Region, s3UrlPrefix);
            this.s3Types = new HashSet<>(Arrays.asList(s3TypesConfig.split(",")));
        } else {
            this.s3StorageService = null;
            this.s3Types = new HashSet<>();
            log.info("S3 未配置，所有文件将使用本地存储");
        }
        log.info("混合存储服务初始化完成 - S3类型: {}, 本地类型: 其他", s3Types);
    }

    @PreDestroy
    public void destroy() {
        if (s3StorageService != null) {
            s3StorageService.destroy();
        }
    }

    @Override
    public String upload(MultipartFile file, String type) {
        if (s3StorageService != null && s3Types.contains(type)) {
            log.info("文件类型 [{}] 路由到 S3 存储", type);
            return s3StorageService.upload(file, type);
        }
        log.info("文件类型 [{}] 路由到本地存储", type);
        return localStorageService.upload(file, type);
    }

    @Override
    public void delete(String url) {
        if (url == null || url.isEmpty()) return;

        if (isS3Url(url) && s3StorageService != null) {
            s3StorageService.delete(url);
        } else {
            localStorageService.delete(url);
        }
    }

    @Override
    public String getType() {
        return "hybrid";
    }

    private boolean isS3Url(String url) {
        return url.startsWith("http");
    }
}
