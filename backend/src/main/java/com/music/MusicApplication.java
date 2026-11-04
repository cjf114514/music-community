package com.music;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot 应用主入口类
 *
 * 整个后端项目从这里启动
 * SpringApplication.run() 会:
 * 1. 启动内嵌的 Tomcat 服务器 (默认端口 8080)
 * 2. 自动扫描并注册所有 @Controller、@Service、@Repository、@Component 等 Bean
 * 3. 根据 application.yml 配置连接数据库、Redis 等
 *
 * @SpringBootApplication 是一个组合注解,等价于同时标注了:
 *   - @SpringBootConfiguration : 标记这是配置类
 *   - @EnableAutoConfiguration : 根据 classpath 下的依赖自动配置 (如:检测到 mysql-connector 就自动配置数据源)
 *   - @ComponentScan            : 扫描当前包及子包下的所有组件
 */
@SpringBootApplication
public class MusicApplication {
    public static void main(String[] args) {
        SpringApplication.run(MusicApplication.class, args);
    }
}
