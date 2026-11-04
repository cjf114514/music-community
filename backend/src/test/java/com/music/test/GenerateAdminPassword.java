package com.music.test;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class GenerateAdminPassword {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String rawPassword = "123456";
        String encodedPassword = encoder.encode(rawPassword);
        
        System.out.println("Generated BCrypt password for '123456':");
        System.out.println(encodedPassword);
        System.out.println("\nLength: " + encodedPassword.length());
        
        // Verify
        boolean matches = encoder.matches(rawPassword, encodedPassword);
        System.out.println("Verification: " + matches);
    }
}
