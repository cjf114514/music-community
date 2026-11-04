package com.music.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JwtUtil 单元测试
 *
 * 为什么优先测这块？
 *   这个项目之前最大的安全问题就是「JWT 密钥写死在代码里」——
 *   任何人都能拿默认密钥伪造任意用户的 Token。
 *   所以这里重点覆盖两条安全底线：
 *     ① 签名不对的 Token 必须失效
 *     ② 密钥不合格时启动自检必须把服务拦下来
 *
 * 测试方式：不启动 Spring 上下文，直接 new 出对象，
 * 用反射把 @Value 注入的字段手动设值（毫秒级完成）。
 *
 * 放在 com.music.util 包下，是为了能直接调用包可见的 validateSecret()。
 */
class JwtUtilTest {

    /** 42 字节，满足 HMAC-SHA256 至少要 32 字节的要求 */
    private static final String STRONG_SECRET = "0123456789abcdefghijklmnopqrstuvwxyzABCD";

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() throws Exception {
        jwtUtil = new JwtUtil();
        setField(jwtUtil, "secret", STRONG_SECRET);
        setField(jwtUtil, "expiration", 86_400_000L);   // 1 天
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = JwtUtil.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    // ==================== 正常签发与解析 ====================

    @Test
    @DisplayName("生成的 Token 能解析出 userId 和 username")
    void generateAndParse() {
        String token = jwtUtil.generateToken(7L, "cjf");

        assertNotNull(token, "Token 不应为空");
        assertEquals(3, token.split("\\.").length, "JWT 应由 header.payload.signature 三段组成");
        assertEquals(7L, jwtUtil.getUserIdFromToken(token));
        assertEquals("cjf", jwtUtil.getUsernameFromToken(token));
        assertFalse(jwtUtil.isTokenExpired(token), "刚签发的 Token 不应过期");
    }

    // ==================== 安全底线 ====================

    @Test
    @DisplayName("过期的 Token 会被识别为过期")
    void expiredTokenIsRejected() throws Exception {
        setField(jwtUtil, "expiration", -1000L);   // 签发即过期
        String token = jwtUtil.generateToken(7L, "cjf");

        assertTrue(jwtUtil.isTokenExpired(token));
    }

    @Test
    @DisplayName("用别的密钥签发的 Token（伪造）会被拒绝")
    void tokenSignedByAnotherSecretIsRejected() throws Exception {
        JwtUtil attacker = new JwtUtil();
        setField(attacker, "secret", "attacker-secret-key-at-least-32-bytes-long!!");
        setField(attacker, "expiration", 86_400_000L);

        String forged = attacker.generateToken(999L, "attacker");

        // 签名验证失败 → 视为无效
        assertTrue(jwtUtil.isTokenExpired(forged), "伪造的 Token 必须被判定为无效");
        assertThrows(Exception.class, () -> jwtUtil.getUserIdFromToken(forged),
                "解析伪造 Token 应当抛异常，而不是返回伪造的 userId");
    }

    @Test
    @DisplayName("被篡改的 Token 会被拒绝")
    void tamperedTokenIsRejected() {
        String token = jwtUtil.generateToken(7L, "cjf");
        // 改动 payload 部分的一个字符，签名就对不上了
        String[] parts = token.split("\\.");
        char[] payload = parts[1].toCharArray();
        payload[0] = payload[0] == 'A' ? 'B' : 'A';
        String tampered = parts[0] + "." + new String(payload) + "." + parts[2];

        assertTrue(jwtUtil.isTokenExpired(tampered));
    }

    // ==================== 启动自检 ====================

    @Test
    @DisplayName("密钥仍是占位符 ${JWT_SECRET} 时，启动自检必须失败")
    void validateSecretRejectsPlaceholder() throws Exception {
        setField(jwtUtil, "secret", "${JWT_SECRET}");

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> jwtUtil.validateSecret());
        assertTrue(ex.getMessage().contains("JWT_SECRET"), "错误提示里应指出缺少哪个环境变量");
    }

    @Test
    @DisplayName("密钥过短（不足 32 字节）时，启动自检必须失败")
    void validateSecretRejectsTooShort() throws Exception {
        setField(jwtUtil, "secret", "short-key");

        assertThrows(IllegalStateException.class, () -> jwtUtil.validateSecret());
    }

    @Test
    @DisplayName("旧的硬编码默认密钥必须被拒绝")
    void validateSecretRejectsLegacyDefault() throws Exception {
        setField(jwtUtil, "secret", "MusicWebsiteSecretKey2024ForJwtTokenGeneration");

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> jwtUtil.validateSecret());
        assertTrue(ex.getMessage().contains("旧版本默认值"));
    }

    @Test
    @DisplayName("合格的 48 字节随机密钥能通过自检")
    void validateSecretAcceptsStrongKey() {
        assertDoesNotThrow(() -> jwtUtil.validateSecret());
    }
}
