package com.music.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义注解 — 标记需要管理员权限的方法
 *
 * 工作原理:
 *   1. 在 Controller 方法上加 @RequireAdmin 注解
 *   2. AdminInterceptor 拦截器会检查每个请求对应的方法是否有这个注解
 *   3. 如果有 → 检查用户角色是否为 ADMIN
 *   4. 如果不是 → 返回 403 "权限不足"
 *
 * 使用示例:
 *   @RequireAdmin
 *   @DeleteMapping("/{id}")
 *   public Result<Void> deleteUser(@PathVariable Long id) { ... }
 *
 * 两个元注解的含义:
 *   @Target(ElementType.METHOD)  — 这个注解只能用在方法上
 *   @Retention(RetentionPolicy.RUNTIME) — 在运行时仍保留 (拦截器通过反射读取)
 */
@Target(ElementType.METHOD)       // 只能加在方法上
@Retention(RetentionPolicy.RUNTIME) // 运行时保留,这样反射才能读到
public @interface RequireAdmin {
}
