package com.music.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT (JSON Web Token) 工具类
 *
 * ========== JWT 是什么？==========
 * 一段经过签名的加密字符串,用户登录后由服务端生成并返回给前端
 * 前端每次请求携带它,服务端通过它识别"你是谁"
 * 格式: xxxxx.yyyyy.zzzzz (三段用 . 分隔)
 *   - Header:  声明签名算法 (如: HS256)
 *   - Payload: 存放用户信息 (如: userId, username)
 *   - Signature: 对前两部分的签名,防篡改
 *
 * ========== 为什么用 JWT 而不是传统 Session？==========
 *   - 无状态: 服务端不需要存储会话,可以任意水平扩展
 *   - 适合前后端分离: Token 存在浏览器 localStorage 中,每次请求通过 Header 发送
 *   - 移动端友好: 不需要 Cookie,直接传字符串
 *
 * ========== 本项目 JWT 流程 ==========
 *   用户登录成功 → AuthService 调用 generateToken() 生成 Token
 *   → 返回给前端 → 前端存入 localStorage
 *   → 后续每次请求, Axios 拦截器自动在 Authorization 头加上 "Bearer {token}"
 *   → JwtInterceptor 调用 parseToken() 解析并验证签名 → 提取 userId
 *   → Controller 通过 request.getAttribute("userId") 拿到当前用户 ID
 *
 * ========== 安全性核心 ==========
 *   signWith(密钥)    → 用密钥对 Header+Payload 做 HMAC-SHA 签名
 *   verifyWith(密钥)  → 解析时用同一个密钥验证,签名不匹配就会失败
 *   结论: 不知道密钥的人无法伪造有效 Token
 */
@Component  // 标注为 Spring Bean,交给 Spring 管理 (这样才能用 @Value 注入配置)
public class JwtUtil {

    /**
     * JWT 签名密钥
     *
     * 必须由运行环境提供（application.yml 里写的是 ${JWT_SECRET}），
     * 这里刻意不留默认值：没配置就启动失败，避免"悄悄用一个人尽皆知的密钥跑起来"。
     *
     * 旧版本这里写的是 ${jwt.secret:MusicWebsiteSecretKey2024ForJwtTokenGeneration}，
     * 那串默认值已经公开在历史里，任何人都能拿它伪造任意用户的 Token，必须废弃。
     */
    @Value("${jwt.secret}")
    private String secret; //密钥

    /**
     * Token 过期时间,单位毫秒
     * 86400000ms = 1000 × 60 × 60 × 24 = 1 天
     */
    @Value("${jwt.expiration:86400000}")
    private long expiration; //过期时间 1天


    /**
     * 将字符串密钥转换为 Java 加密库可用的 SecretKey 对象
     *
     * StandardCharsets.UTF_8:
     *   明确指定把字符串转成字节数组时使用 UTF-8 编码
     *   不指定的话会用系统默认编码,可能导致不同操作系统行为不一致
     *
     * Keys.hmacShaKeyFor():
     *   根据密钥字节数组,生成符合 HMAC-SHA 算法要求的安全密钥对象
     *   HMAC-SHA = 用哈希函数 (SHA-256) 做消息认证码
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 启动自检 —— 防止「密钥没配」却悄悄用一个可猜的字符串跑起来
     *
     * 为什么必须自己检查？
     *   Spring 解析 YAML 里的 ${JWT_SECRET} 时，如果这个环境变量不存在，
     *   并不会抛异常，而是**把 "${JWT_SECRET}" 这个字面量原样留下**。
     *   结果就是：密钥变成了人尽皆知的字符串，谁都能伪造 Token，而且毫无报错。
     *   所以这里显式兜住，宁可启动失败，也不要裸奔。
     *
     * @PostConstruct: 在所有 @Value 注入完成之后、Bean 可被使用之前执行
     */
    @jakarta.annotation.PostConstruct
    void validateSecret() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "JWT 密钥未配置：请在 .env 里设置 JWT_SECRET（本地），或在服务器 /etc/music/env 中配置");
        }
        if (secret.contains("${")) {
            throw new IllegalStateException(
                    "JWT 密钥未生效：JWT_SECRET 环境变量没有注入，当前值仍是占位符 " + secret
                            + "。本地请用 run-local.ps1 启动，或检查 IDE 运行配置的环境变量。");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "JWT 密钥太短（" + secret.getBytes(StandardCharsets.UTF_8).length
                            + " 字节），HMAC-SHA256 要求至少 32 字节。建议用 48 字节随机值。");
        }
        if (secret.contains("MusicWebsiteSecretKey2024")) {
            throw new IllegalStateException(
                    "JWT 密钥仍在用旧版本默认值，该值已公开在代码历史中，必须更换");
        }
    }

    /**
     * 生成 JWT Token (登录成功时调用)
     *
     * @param userId   用户 ID,存入 Payload 的自定义字段
     * @param username 用户名,同时存入 claims 和 subject
     * @return 签好名的 JWT 字符串
     *
     * Token 内部结构示例:
     *   Payload:
     *   {
     *     "userId": 1,         ← 自定义字段
     *     "username": "zhangsan", ← 自定义字段
     *     "sub": "zhangsan",  ← 标准字段: Token 主题/所有者
     *     "iat": 1716307200,   ← 标准字段: 签发时间
     *     "exp": 1716393600    ← 标准字段: 过期时间 (签发时间 + 1 天)
     *   }
     *
     * 链式调用解读 (Jwts.builder() 使用建造者模式):
     *   .claims(map)    → 放入自定义的键值对
     *   .subject(name)  → 设置 Token 主题
     *   .issuedAt(now)  → 签发时间
     *   .expiration(ex) → 过期时间
     *   .signWith(key)  → 用密钥签名 (最核心的一步,防篡改)
     *   .compact()      → 把三部分 (Header+Payload+Signature) 打包成最终字符串
     */
    public String generateToken(Long userId, String username) {
        // claims = JWT 的 Payload,存储你想要携带的用户信息
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);

        return Jwts.builder()
                .claims(claims) //把用户信息放入 token
                .subject(username) //subject = token 的"主题",通常存用户名或用户 ID
                .issuedAt(new Date()) //签发时间
                .expiration(new Date(System.currentTimeMillis() + expiration)) //过期时间 = 当前时间 + 1 天
                .signWith(getSigningKey()) //签名 — 谁不知道密钥,谁就伪造不了 Token
                .compact(); //打包生成最终的 JWT 字符串
    }

    /**
     * 解析并验证 JWT Token (拦截器每次校验时调用)
     *
     * @param token JWT 字符串
     * @return Claims 对象 (Payload 里的所有数据)
     * @throws io.jsonwebtoken.JwtException 签名不匹配 / Token 过期 / 格式错误
     *
     * 解析过程:
     *   1. parser() → 创建解析器
     *   2. verifyWith(key) → 设置验证密钥 (和签名时用同一个密钥)
     *   3. build() → 构建解析器实例
     *   4. parseSignedClaims(token) → 解析并验证: 签名不对 / 过期 → 直接抛异常
     *   5. getPayload() → 取出 Payload 里的所有字段
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey()) //验证签名 — 和生成时用的密钥一致才能通过
                .build()
                .parseSignedClaims(token) //解析 JWT 字符串
                .getPayload(); //返回 Payload 中的用户信息
    }

    /**
     * 从 Token 中提取用户 ID
     *
     * 使用场景: 拦截器解析完 Token,把 userId 存入 request Attribute
     *          Controller 拿到 userId 后就知道"当前是谁在操作"
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("userId", Long.class); // get(键, 值类型)
    }

    /**
     * 从 Token 中提取用户名
     *
     * getSubject() 返回的是生成时 .subject(username) 设入的值
     */
    public String getUsernameFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.getSubject();
    }

    /**
     * 判断 Token 是否过期
     *
     * @return true = 已过期/无效, false = 未过期
     *
     * try-catch 的作用:
     *   parseToken() 抛出任何异常 (签名错误/格式不对/已过期)
     *   → 说明这个 Token 不可用 → 返回 true (当做过期处理)
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = parseToken(token);
            // getExpiration() 返回过期时间, before(new Date()) = "过期时间 < 当前时间" 即为已过期
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return true; //解析失败 = Token 无效,视为过期
        }
    }
}
