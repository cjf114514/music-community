package com.music.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.UUID;

@Slf4j
public class S3StorageService implements StorageService {

    private final String endpoint;
    private final String accessKey;
    private final String secretKey;
    private final String bucketName;
    private final String region;
    private final String customUrlPrefix;
    private S3Client s3Client;

    public S3StorageService(String endpoint, String accessKey, String secretKey,
                            String bucketName, String region, String customUrlPrefix) {
        this.endpoint = endpoint;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.bucketName = bucketName;
        this.region = region;
        this.customUrlPrefix = customUrlPrefix;
        init();
    }

    private void init() {
        log.info("初始化 S3 客户端 - endpoint: {}, bucket: {}, region: {}", endpoint, bucketName, region);

        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey.trim(), secretKey.trim());

        S3Configuration s3Config = S3Configuration.builder()
                .pathStyleAccessEnabled(true)
                .chunkedEncodingEnabled(false)
                .build();

        s3Client = S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .region(Region.US_EAST_1)
                .serviceConfiguration(s3Config)
                .build();

        log.info("S3 存储客户端初始化成功, endpoint: {}, bucket: {}", endpoint, bucketName);
    }

    public void destroy() {
        if (s3Client != null) {
            s3Client.close();
            log.info("S3 存储客户端已关闭");
        }
    }

    @Override
    public String upload(MultipartFile file, String type) {
        if (file.isEmpty()) {
            throw new RuntimeException("文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : "";

        String objectKey = type + "/" + UUID.randomUUID().toString() + extension;

        try (InputStream inputStream = file.getInputStream()) {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .contentType(getContentType(extension))
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromInputStream(inputStream, file.getSize()));

            String url = getFileUrl(objectKey);
            log.info("S3 上传成功: {}", url);
            return url;
        } catch (IOException e) {
            log.error("S3 上传失败", e);
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("S3 上传失败 - endpoint: {}, bucket: {}, region: {}", endpoint, bucketName, region, e);
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }

    @Override
    public void delete(String url) {
        if (url == null || url.isEmpty()) return;

        try {
            String objectKey = extractObjectKey(url);
            if (objectKey != null) {
                DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                        .bucket(bucketName)
                        .key(objectKey)
                        .build();
                s3Client.deleteObject(deleteRequest);
                log.info("S3 删除成功: {}", objectKey);
            }
        } catch (Exception e) {
            log.warn("S3 删除失败: {}", url, e);
        }
    }

    @Override
    public String getType() {
        return "s3";
    }

    private String getFileUrl(String objectKey) {
        if (customUrlPrefix != null && !customUrlPrefix.isEmpty()) {
            String prefix = customUrlPrefix.endsWith("/") ? customUrlPrefix : customUrlPrefix + "/";
            return prefix + objectKey;
        }
        String endpointPrefix = endpoint.endsWith("/") ? endpoint : endpoint + "/";
        return endpointPrefix + bucketName + "/" + objectKey;
    }

    private String extractObjectKey(String url) {
        if (url == null) return null;

        if (customUrlPrefix != null && !customUrlPrefix.isEmpty()) {
            String prefix = customUrlPrefix.endsWith("/") ? customUrlPrefix : customUrlPrefix + "/";
            if (url.startsWith(prefix)) {
                return url.substring(prefix.length());
            }
        }

        String bucketPrefix = endpoint + "/" + bucketName + "/";
        if (url.startsWith(bucketPrefix)) {
            return url.substring(bucketPrefix.length());
        }

        if (url.contains("/" + bucketName + "/")) {
            int idx = url.indexOf("/" + bucketName + "/") + bucketName.length() + 2;
            return url.substring(idx);
        }

        return null;
    }

    private String getContentType(String extension) {
        if (extension == null) return "application/octet-stream";

        return switch (extension.toLowerCase()) {
            case ".mp3" -> "audio/mpeg";
            case ".wav" -> "audio/wav";
            case ".flac" -> "audio/flac";
            case ".aac" -> "audio/aac";
            case ".ogg" -> "audio/ogg";
            case ".m4a" -> "audio/mp4";
            case ".jpg", ".jpeg" -> "image/jpeg";
            case ".png" -> "image/png";
            case ".gif" -> "image/gif";
            case ".webp" -> "image/webp";
            case ".lrc" -> "text/plain";
            default -> "application/octet-stream";
        };
    }
}
