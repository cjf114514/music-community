package com.music.controller;

import com.music.common.Result;
import com.music.repository.UserRepository;
import com.music.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/email")
@RequiredArgsConstructor
public class EmailController {

    private final EmailService emailService;
    private final StringRedisTemplate stringRedisTemplate;
    private final UserRepository userRepository;

    private static final String EMAIL_CODE_PREFIX = "email:code:";
    private static final long CODE_EXPIRE_MINUTES = 5;

    @PostMapping("/send-code")
    public Result<Void> sendVerificationCode(@RequestParam String email) {
        if (!email.matches("^[1-9]\\d{4,10}@qq\\.com$")) {
            return Result.error("请输入正确的QQ邮箱");
        }

        if (userRepository.existsByEmail(email)) {
            return Result.error("该邮箱已被注册");
        }

        String code = String.format("%06d", new Random().nextInt(1000000));
        stringRedisTemplate.opsForValue().set(EMAIL_CODE_PREFIX + email, code, CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);

        emailService.sendVerificationCode(email, code);
        return Result.success();
    }

    @PostMapping("/send-reset-code")
    public Result<Void> sendResetCode(@RequestParam String email) {
        if (!email.matches("^[1-9]\\d{4,10}@qq\\.com$")) {
            return Result.error("请输入正确的QQ邮箱");
        }

        String code = String.format("%06d", new Random().nextInt(1000000));
        stringRedisTemplate.opsForValue().set(EMAIL_CODE_PREFIX + email, code, CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);

        emailService.sendResetCode(email, code);
        return Result.success();
    }

    @PostMapping("/send-bind-code")
    public Result<Void> sendBindCode(@RequestParam String email) {
        if (!email.matches("^[1-9]\\d{4,10}@qq\\.com$")) {
            return Result.error("请输入正确的QQ邮箱");
        }

        String code = String.format("%06d", new Random().nextInt(1000000));
        stringRedisTemplate.opsForValue().set(EMAIL_CODE_PREFIX + email, code, CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);

        emailService.sendBindCode(email, code);
        return Result.success();
    }
}
