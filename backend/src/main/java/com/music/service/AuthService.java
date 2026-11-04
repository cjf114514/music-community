package com.music.service;

import com.music.dto.LoginRequest;
import com.music.dto.LoginResponse;
import com.music.dto.RegisterRequest;
import com.music.entity.User;
import com.music.repository.UserRepository;
import com.music.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 认证服务 — 处理登录、注册、密码管理
 *
 * ========== 本类在项目中的位置 ==========
 * AuthController 接收前端请求 → AuthService 处理业务逻辑 → UserRepository 操作数据库
 *
 * ========== 用到的外部依赖 ==========
 *   UserRepository      → 查数据库 user 表
 *   JwtUtil             → 生成 JWT Token
 *   StringRedisTemplate  → 操作 Redis (存验证码、登录失败计数)
 *   BCryptPasswordEncoder → 密码加密和校验
 *
 * @Service → 标记为 Spring Service Bean
 * @RequiredArgsConstructor → 为所有 final 字段生成构造方法,Spring 自动注入依赖
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate stringRedisTemplate;  // Redis 客户端(简化字符串操作版)

    /**
     * BCrypt 密码编码器
     * static final → 全局唯一实例,不随对象变化
     *
     * BCrypt 特点:
     *   1. 自带盐值(salt): 同一密码每次加密结果不同,彩虹表攻击无效
     *   2. 计算慢(可配置): 故意做得慢,拖慢暴力破解速度
     *   3. 不可逆: 无法从哈希值反推明文密码
     *
     * 用法:
     *   encode("123456")  → "$2a$10$..." (60 字符的哈希串)
     *   matches("123456", hash) → true/false
     */
    private static final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // ==================== Redis 键前缀 ====================

    /** Redis 键前缀: 图片验证码 → captcha:{uuid} */
    private static final String CAPTCHA_KEY_PREFIX = "captcha:";

    /** Redis 键前缀: 登录失败次数 → login:fail:{username} */
    private static final String LOGIN_FAIL_KEY_PREFIX = "login:fail:";

    /** Redis 键前缀: 登录锁定标记 → login:lock:{username} */
    private static final String LOGIN_LOCK_KEY_PREFIX = "login:lock:";

    /** Redis 键前缀: 邮箱验证码 → email:code:{email} */
    private static final String EMAIL_CODE_PREFIX = "email:code:";

    // ==================== 安全策略常量 ====================

    /** 最大允许登录失败次数 (连续失败 5 次就锁定) */
    private static final int MAX_FAIL_COUNT = 5;

    /** 登录锁定时间: 1 分钟 */
    private static final long LOCK_DURATION_MINUTES = 1;


    // ==================== 登录 ====================

    /**
     * 登录方法
     *
     * 登录流程:
     *   1. 校验图片验证码 (从 Redis 取 → 比对 → 删除)
     *   2. 检查是否被锁定 (Redis 中有 lock 键则拒绝)
     *   3. 查数据库找用户 → 不存在则拒绝
     *   4. BCrypt 密码比对 → 不匹配则增加失败计数 → 失败 5 次则锁定 1 分钟
     *   5. 密码正确 → 清除失败计数 → 生成 JWT Token → 返回
     *
     * @param request 前端传来的 LoginRequest (用户名+密码+验证码)
     * @return LoginResponse (Token + 用户信息)
     */
    public LoginResponse login(LoginRequest request) {

        // --- 第 1 步: 验证码校验 ---
        // 从 Redis 取出验证码,key 格式: captcha:{验证码key}
        String storedCode = stringRedisTemplate.opsForValue()
                .get(CAPTCHA_KEY_PREFIX + request.getCaptchaKey());
        if (storedCode == null || !storedCode.equalsIgnoreCase(request.getCaptchaCode())) {
            throw new RuntimeException("验证码错误或已过期");
        }
        // 验证码只能用一次,用完就删
        stringRedisTemplate.delete(CAPTCHA_KEY_PREFIX + request.getCaptchaKey());

        // --- 第 2 步: 登录锁定检查 (防暴力破解) ---
        String lockKey = LOGIN_LOCK_KEY_PREFIX + request.getUsername();
        Boolean isLocked = stringRedisTemplate.hasKey(lockKey);
        if (Boolean.TRUE.equals(isLocked)) {
            Long ttl = stringRedisTemplate.getExpire(lockKey, TimeUnit.SECONDS);
            throw new RuntimeException("账户已被锁定，请" + ttl + "秒后重试");
        }

        // --- 第 3 步: 查用户 ---
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("用户名或密码错误"));

        // --- 第 4 步: BCrypt 密码校验 ---
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            incrementFailCount(request.getUsername());  // 密码错 → 增加失败计数
            throw new RuntimeException("用户名或密码错误");
        }

        // --- 第 5 步: 检查账号是否被禁用 ---
        // status = 0 表示管理员禁用了此账号
        if (user.getStatus() == 0) {
            throw new RuntimeException("账号已被禁用");
        }

        // --- 第 6 步: 登录成功 ---
        // 清除失败计数 (防止下次登录被误锁)
        stringRedisTemplate.delete(LOGIN_FAIL_KEY_PREFIX + request.getUsername());

        // 生成 JWT Token (userId 和 username 存入 Payload)
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());

        // 返回登录响应 (Token + 用户信息)
        return new LoginResponse(token, user.getId(), user.getId(),
                user.getUsername(), user.getNickname(), user.getAvatar(), user.getRole());
    }

    /**
     * 登录失败次数累加 + 锁定逻辑
     *
     * 工作流程:
     *   Redis.INCR login:fail:zhangsan → 次数 +1
     *   第 1 次失败 → 设 TTL = 30 分钟 (30 分钟后自动清零)
     *   第 5 次失败 → 写入 lock 键 (TTL = 1 分钟) → 删除 fail 键
     *
     * 为什么用 Redis 而不是数据库？
     *   1. Redis 自带 TTL (过期自动删除) — 不需要定时任务清理
     *   2. 读写极快 — 登录是高频操作,不能慢
     *   3. 数据不重要 — 失败计数丢了也无所谓
     */
    private void incrementFailCount(String username) {
        String failKey = LOGIN_FAIL_KEY_PREFIX + username;

        // INCR: 如果 key 不存在 → 创建并设为 1; 如果 key 存在 → 值 +1
        Long failCount = stringRedisTemplate.opsForValue().increment(failKey);
        if (failCount == null) return;

        // 第一次失败时设置 Redis key 的过期时间 (30 分钟)
        // 如果不设,这个 key 会永远存在,用户 30 分钟后再输错就直接锁定了
        if (failCount == 1) {
            stringRedisTemplate.expire(failKey, 30, TimeUnit.MINUTES);
        }

        // 失败次数达到阈值 → 锁定账户
        if (failCount >= MAX_FAIL_COUNT) {
            String lockKey = LOGIN_LOCK_KEY_PREFIX + username;
            // 写入锁定标记,1 分钟后自动删除
            stringRedisTemplate.opsForValue().set(lockKey, "1",
                    LOCK_DURATION_MINUTES, TimeUnit.MINUTES);
            // 删除失败计数 (锁定期间不需要它了)
            stringRedisTemplate.delete(failKey);
        }
    }


    // ==================== 注册 ====================

    /**
     * 用户注册
     *
     * 流程:
     *   1. 校验邮箱验证码 (从 Redis 取 → 比对 → 删除)
     *   2. 检查用户名和邮箱是否已存在
     *   3. BCrypt 加密密码 (不存明文!)
     *   4. 保存到数据库
     */
    public User register(RegisterRequest request) {
        // 校验邮箱验证码
        String storedCode = stringRedisTemplate.opsForValue()
                .get(EMAIL_CODE_PREFIX + request.getEmail());
        if (storedCode == null) {
            throw new RuntimeException("验证码已过期，请重新获取");
        }
        if (!storedCode.equals(request.getEmailCode())) {
            throw new RuntimeException("验证码错误");
        }
        stringRedisTemplate.delete(EMAIL_CODE_PREFIX + request.getEmail());

        // 唯一性校验
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("该邮箱已被注册");
        }

        // 构建用户对象
        User user = new User();
        user.setUsername(request.getUsername());
        // 密码必须加密存储 — 即使数据库泄露,攻击者也拿不到明文密码
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setNickname(request.getNickname() != null
                ? request.getNickname() : request.getUsername());  // 没填昵称就用用户名
        user.setStatus(1);  // 默认正常状态

        return userRepository.save(user);
    }


    // ==================== 用户信息管理 ====================

    /** 根据 ID 获取用户信息 */
    public User getUserInfo(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
    }

    /**
     * 修改密码 (已登录状态)
     *
     * 流程:
     *   1. 验证旧密码是否正确 (防止他人盗用已登录账号)
     *   2. 验证新密码长度 (6-20 位)
     *   3. BCrypt 加密新密码 → 保存
     */
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("原密码错误");
        }
        if (newPassword.length() < 6 || newPassword.length() > 20) {
            throw new RuntimeException("新密码长度需为6-20位");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    /**
     * 重置密码 (未登录状态 — 通过邮箱验证码)
     * 适用场景: 用户忘记密码
     */
    public void resetPassword(String email, String code, String newPassword) {
        // 校验邮箱验证码
        String storedCode = stringRedisTemplate.opsForValue()
                .get(EMAIL_CODE_PREFIX + email);
        if (storedCode == null) {
            throw new RuntimeException("验证码已过期，请重新获取");
        }
        if (!storedCode.equals(code)) {
            throw new RuntimeException("验证码错误");
        }
        stringRedisTemplate.delete(EMAIL_CODE_PREFIX + email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("该邮箱未绑定任何账号"));

        if (newPassword.length() < 6 || newPassword.length() > 20) {
            throw new RuntimeException("新密码长度需为6-20位");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    /**
     * 更换绑定邮箱 (已登录状态)
     * 需要验证新邮箱的验证码,防止绑到别人的邮箱
     */
    public void changeEmail(Long userId, String newEmail, String code) {
        String storedCode = stringRedisTemplate.opsForValue()
                .get(EMAIL_CODE_PREFIX + newEmail);
        if (storedCode == null) {
            throw new RuntimeException("验证码已过期，请重新获取");
        }
        if (!storedCode.equals(code)) {
            throw new RuntimeException("验证码错误");
        }
        stringRedisTemplate.delete(EMAIL_CODE_PREFIX + newEmail);

        if (userRepository.existsByEmail(newEmail)) {
            throw new RuntimeException("该邮箱已被其他用户绑定");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        user.setEmail(newEmail);
        userRepository.save(user);
    }

    /**
     * 更新用户个人信息 (昵称、头像、性别、生日、签名)
     *
     * 只更新不为 null 的字段 → 前端可以只传改动的字段
     * 这个技巧避免了"必须传所有字段"的麻烦
     */
    public User updateUserInfo(Long userId, User userInfo) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (userInfo.getNickname() != null) user.setNickname(userInfo.getNickname());
        if (userInfo.getAvatar() != null) user.setAvatar(userInfo.getAvatar());
        if (userInfo.getGender() != null) user.setGender(userInfo.getGender());
        if (userInfo.getBirthday() != null) user.setBirthday(userInfo.getBirthday());
        if (userInfo.getSignature() != null) user.setSignature(userInfo.getSignature());

        return userRepository.save(user);
    }
}
