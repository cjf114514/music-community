package com.music.test;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class GenerateValidPassword {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String rawPassword = "123456";
        
        // 生成 3 个不同的 BCrypt 密码
        for (int i = 0; i < 3; i++) {
            String hash = encoder.encode(rawPassword);
            System.out.println("Hash " + (i+1) + ": " + hash);
            System.out.println("Length: " + hash.length());
            System.out.println("Valid: " + encoder.matches(rawPassword, hash));
            System.out.println();
        }
    }
}
