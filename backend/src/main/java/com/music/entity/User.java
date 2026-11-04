package com.music.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户实体类 — 映射到数据库 user 表
 *
 * ========== JPA 实体类基础知识 ==========
 * 实体类 = 一个 Java 类对应数据库里的一张表
 * 属性 = 对应表中的列
 * 一个实体对象 = 表中的一行记录
 *
 * JPA 的核心思想: 用操作对象的方式操作数据库 (ORM: Object-Relational Mapping)
 *   不需要手写 SQL → JPA 自动生成 INSERT/UPDATE/SELECT/DELETE
 *   例如: userRepository.save(user) → JPA 自动生成 INSERT 或 UPDATE 语句
 *
 * @Entity 和 @Table 的区别:
 *   @Entity 标记"这是一个 JPA 实体类"(必须)
 *   @Table  指定映射的表名(可选,不写则默认类名作表名)
 *
 * Lombok @Data 的作用:
 *   自动生成: getter/setter、toString()、equals()、hashCode()
 *   省去大量样板代码,让实体类保持简洁
 *   编译时生成,不影响运行时性能
 */
@Data
@Entity
@Table(name = "user")
public class User {

    /**
     * 主键 ID
     * @Id → 标记为主键
     * @GeneratedValue → 指定主键生成策略
     *   IDENTITY = 自增 (MySQL 的 AUTO_INCREMENT)
     *   SEQUENCE = 序列 (Oracle/PostgreSQL)
     *   AUTO     = JPA 自动选择 (默认)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 用户名 — 非空、唯一、最长 50 字符 */
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    /**
     * 密码 — 存储的是 BCrypt 加密后的哈希值,不是明文
     * BCrypt 哈希特点: 同一密码两次加密结果不同(自带随机盐)
     *
     * @JsonIgnore — 密码永远不参与 JSON 转换:
     *   1. 任何返回 User 的接口都不会再把 password 吐给前端
     *   2. 前端也无法通过 PUT /api/auth/info 把 password 混进更新里
     *   只影响 JSON 序列化/反序列化,不影响 JPA 读写数据库,也不影响 BCrypt 比对
     */
    @JsonIgnore
    @Column(nullable = false, length = 255)
    private String password;

    /** 邮箱 — 唯一,用于找回密码和发送通知 */
    @Column(length = 100, unique = true)
    private String email;

    /** 头像 URL */
    @Column(length = 255)
    private String avatar;

    /** 昵称 — 显示在评论、社区等位置 */
    @Column(length = 50)
    private String nickname;

    /**
     * 性别: 0=未知(默认), 1=男, 2=女
     * 没有 @Column 注解 → 列名默认 = 字段名 "gender"
     */
    private Integer gender;

    /** 生日 */
    private LocalDate birthday;

    /** 个性签名 */
    @Column(length = 200)
    private String signature;

    /**
     * 账号状态: 0=禁用, 1=正常(默认)
     */
    private Integer status;

    /**
     * 用户角色: "USER"(普通用户,默认), "ADMIN"(管理员)
     * 管理员可以访问 /api/admin/** 下的管理接口
     */
    @Column(length = 20)
    private String role;

    /**
     * 创建时间 — updatable = false 含义:
     *   这个字段在 UPDATE 时不会被更新,JPA 生成的 UPDATE 语句不会包含此列
     *   只能设一次,之后改不了
     */
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    /** 更新时间 — 每次修改记录时自动更新为当前时间 */
    @Column(name = "update_time")
    private LocalDateTime updateTime;

    /**
     * JPA 生命周期回调 — 插入前自动执行
     *
     * @PrePersist 注解的方法在 JPA 执行 INSERT 之前自动调用
     * 用于设置默认值和时间戳,不用在每个 Service 方法里手动设
     *
     * 何时触发: userRepository.save(新用户对象) 时
     */
    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        updateTime = LocalDateTime.now();
        if (gender == null) gender = 0;       // 默认性别: 未知
        if (status == null) status = 1;       // 默认状态: 正常
        if (role == null || role.isEmpty()) role = "USER";  // 默认角色: 普通用户
    }

    /**
     * JPA 生命周期回调 — 更新前自动执行
     *
     * @PreUpdate 注解的方法在 JPA 执行 UPDATE 之前自动调用
     * 何时触发: userRepository.save(已有用户对象) 时
     */
    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();  // 每次更新都刷新时间戳
    }
}
