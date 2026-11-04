package com.music.controller;

import com.google.code.kaptcha.impl.DefaultKaptcha;
import com.music.common.Result;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

@RestController
@RequestMapping("/api/captcha")
@RequiredArgsConstructor
public class CaptchaController {

    private final DefaultKaptcha defaultKaptcha;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String CAPTCHA_KEY_PREFIX = "captcha:";
    private static final long CAPTCHA_EXPIRE_MINUTES = 5;

    @GetMapping("/generate")
    public void generateCaptcha(@RequestParam String key, HttpServletResponse response) throws IOException {
        String code = defaultKaptcha.createText();

        stringRedisTemplate.opsForValue().set(
            CAPTCHA_KEY_PREFIX + key,
            code,
            CAPTCHA_EXPIRE_MINUTES,
            TimeUnit.MINUTES
        );

        response.setContentType("image/jpeg");
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);

        BufferedImage image = defaultKaptcha.createImage(code);
        ServletOutputStream outputStream = response.getOutputStream();
        ImageIO.write(image, "jpg", outputStream);
        outputStream.flush();
        outputStream.close();
    }
}
