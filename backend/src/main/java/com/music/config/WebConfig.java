package com.music.config;

import com.music.interceptor.AdminInterceptor;
import com.music.interceptor.JwtInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 层总配置文件
 *
 * 这个类是整个 Web 层的"控制中心",配置了三件事:
 * 1. CORS 跨域 — 允许前端 (不同端口) 访问后端接口
 * 2. 拦截器注册 — 请求先经过拦截器校验身份,再进入 Controller
 * 3. 静态资源映射 — 让上传的文件 (mp3、封面图等) 能通过 URL 直接访问
 *
 * implements WebMvcConfigurer:
 *   不是继承,而是实现接口 → 可以选择性覆盖需要的方法 (继承则必须重写所有方法)
 *
 * @RequiredArgsConstructor:
 *   Lombok 注解,为所有 final 字段自动生成构造函数
 *   等价于手写:
 *     public WebConfig(JwtInterceptor ji, AdminInterceptor ai) {
 *         this.jwtInterceptor = ji;
 *         this.adminInterceptor = ai;
 *     }
 *   Spring 会自动注入构造函数需要的 Bean (依赖注入)
 */
@Configuration  // 标记这是配置类,Spring Boot 启动时会加载
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;       // JWT 认证拦截器
    private final AdminInterceptor adminInterceptor;    // 管理员权限拦截器

    @Value("${app.upload-dir}")  // 从 application.yml 读取上传目录路径 (如: ./uploads)
    private String uploadDir;

    /**
     * 本地音乐库根目录（可选）
     *
     * 配了它就把整个目录挂到 /library/** 上，音频文件不需要复制到 uploads，
     * 浏览器直接用 /library/<相对路径> 播放。留空则该能力关闭。
     */
    @Value("${app.library.audio-dir:}")
    private String libraryAudioDir;

    /**
     * ========== 1. CORS (跨域资源共享) 配置 ==========
     *
     * 为什么需要 CORS?
     *   前端开发时运行在 localhost:5173, 后端在 localhost:8080
     *   浏览器同源策略禁止不同"源"之间的 AJAX 请求
     *   CORS 配置告诉浏览器:"来自其他端口的请求是合法的"
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")              // 对所有接口生效
                .allowedOriginPatterns("*")      // 允许任意来源 (开发环境;生产要限制)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")  // 允许的 HTTP 方法
                .allowedHeaders("*")             // 允许任意请求头
                .allowCredentials(true)           // 允许携带 Cookie/Token
                .maxAge(3600);                   // 预检请求缓存 1 小时 (避免每次发 OPTIONS)
    }

    /**
     * ========== 2. 拦截器注册 ==========
     *
     * 执行顺序: 请求 → JwtInterceptor → AdminInterceptor → Controller
     *
     * Ant 风格路径匹配:
     *   /api/*      匹配 /api/playlists, /api/songs (一层)
     *   /api/**     匹配 /api/ 下所有路径 (任意层)
     *   /api/* /songs 匹配 /api/123/songs (两层,中间 * = 任意一段)
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // --- JWT 拦截器 ---
        // 拦截所有 /api/** 请求,但排除白名单中的公开接口
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/**")       // 拦截所有 API 请求
                .excludePathPatterns(              // 排除以下路径 (游客可以直接访问):
                        // 登录注册相关
                        "/api/auth/login",
                        "/api/auth/register",
                        "/api/captcha/generate",
                        "/api/email/send-code",
                        // 歌曲相关 (只读)
                        "/api/songs",
                        "/api/songs/search",
                        "/api/songs/random",
                        "/api/songs/*",            // /api/songs/1 → 单首歌曲详情
                        "/api/songs/*/lyric",       // /api/songs/1/lyric → 歌词
                        // 歌单相关 (只读)
                        "/api/playlists",
                        "/api/playlists/*",         // 歌单详情、某个用户的歌单等
                        "/api/playlists/*/songs",   // 歌单中的歌曲列表
                        "/api/playlists/search",
                        // 排行榜、搜索、歌手、专辑等公开内容
                        "/api/play-history/ranking",
                        "/api/search/hot",
                        "/api/search/users",
                        "/api/singers",
                        "/api/singers/*",
                        "/api/singers/*/songs",
                        "/api/albums/*",
                        // 社区公开内容
                        "/api/community/posts",
                        "/api/community/search",
                        "/api/community/posts/*/comments",
                        // 公告
                        "/api/announcements/**"
                );

        // --- 管理员拦截器 ---
        // 只拦截 /api/admin/** 下的请求
        registry.addInterceptor(adminInterceptor)
                .addPathPatterns("/api/admin/**");
    }

    /**
     * ========== 3. 静态资源映射 ==========
     *
     * 把本地的 ./uploads 目录映射到 URL /uploads/
     * 例如: 文件 ./uploads/cover/abc.jpg → 浏览器访问 http://localhost:8080/uploads/cover/abc.jpg
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")              // URL 路径模式
                .addResourceLocations("file:" + uploadDir + "/"); // 映射到本地文件夹

        // 本地音乐库：把配置的音乐目录直接挂成 /library/**，免去复制文件的几百 MB 开销
        if (libraryAudioDir != null && !libraryAudioDir.isBlank()) {
            String base = libraryAudioDir.replace('\\', '/');
            if (!base.endsWith("/")) base += "/";
            registry.addResourceHandler("/library/**")
                    .addResourceLocations("file:" + base);
        }
    }
}
