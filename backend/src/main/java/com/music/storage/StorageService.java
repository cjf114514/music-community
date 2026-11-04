package com.music.storage;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {
    
    String upload(MultipartFile file, String type);
    
    void delete(String url);
    
    String getType();
}
