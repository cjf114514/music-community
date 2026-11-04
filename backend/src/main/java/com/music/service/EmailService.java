package com.music.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender javaMailSender;

    /**
     * 发件人地址 —— 从配置读取，不写死在代码里
     *
     * 原因：写死会把个人邮箱永久记录进仓库（谁 clone 都能看到）。
     * 现在它来自 spring.mail.username，而那个值最终来自环境变量 MAIL_USERNAME。
     */
    @Value("${spring.mail.username}")
    private String fromAddress;

    /** 发件人显示名称，可在 yml 里用 app.mail.from-name 覆盖 */
    @Value("${app.mail.from-name:在线音乐}")
    private String fromName;

    private void sendCodeEmail(String toEmail, String code, String subject, String title) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress, fromName);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText("<div style='font-family:Arial,sans-serif;padding:20px;'>"
                    + "<h2 style='color:#667eea;'>在线音乐</h2>"
                    + "<p>您的" + title + "为：</p>"
                    + "<div style='font-size:32px;font-weight:bold;color:#764ba2;letter-spacing:8px;margin:20px 0;'>"
                    + code + "</div>"
                    + "<p style='color:#999;font-size:12px;'>验证码5分钟内有效，请勿泄露给他人。</p>"
                    + "</div>", true);
            javaMailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("邮件发送失败：" + e.getMessage());
        }
    }

    public void sendVerificationCode(String toEmail, String code) {
        sendCodeEmail(toEmail, code, "在线音乐 - 注册验证码", "注册验证码");
    }

    public void sendResetCode(String toEmail, String code) {
        sendCodeEmail(toEmail, code, "在线音乐 - 密码重置验证码", "密码重置验证码");
    }

    public void sendBindCode(String toEmail, String code) {
        sendCodeEmail(toEmail, code, "在线音乐 - 邮箱绑定验证码", "邮箱绑定验证码");
    }
}
