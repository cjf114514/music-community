package com.music.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Slf4j
public class LocalStorageService implements StorageService {

    private final String uploadDir;
    private final String urlPrefix;

    public LocalStorageService(String uploadDir, String urlPrefix) {
        this.uploadDir = uploadDir;
        this.urlPrefix = urlPrefix;
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

        try {
            String fileName = UUID.randomUUID().toString() + extension;
            Path dirPath = Paths.get(uploadDir, type);
            Files.createDirectories(dirPath);

            Path filePath = dirPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            return urlPrefix + "/" + type + "/" + fileName;
        } catch (IOException e) {
            log.error("本地存储上传失败", e);
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }

    @Override
    public void delete(String url) {
        if (url == null || url.isEmpty()) return;

        try {
            String fileName = url.substring(url.lastIndexOf("/") + 1);
            String type = url.substring(url.indexOf(urlPrefix) + urlPrefix.length() + 1, url.lastIndexOf("/"));
            Path filePath = Paths.get(uploadDir, type, fileName);
            Files.deleteIfExists(filePath);
        } catch (Exception e) {
            log.warn("删除文件失败: {}", url, e);
        }
    }

    @Override
    public String getType() {
        return "local";
    }
}
