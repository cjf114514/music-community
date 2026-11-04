package com.music.exception;

import com.music.common.Result;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 *
 * ========== 为什么需要它？==========
 * Controller 方法中如果抛出了异常没有 try-catch:
 *   没有这个处理器 → Spring 返回默认的 500 错误页面/JSON (内容不可控)
 *   有了这个处理器 → 自动捕获异常 → 返回格式统一的 Result.error()
 *
 * ========== 工作原理 ==========
 * @RestControllerAdvice = @ControllerAdvice + @ResponseBody
 *   作用: 拦截所有 @RestController 抛出的异常
 *
 * @ExceptionHandler(RuntimeException.class)
 *   作用: 专门处理 RuntimeException 及其子类
 *   方法参数中的异常对象 = 实际抛出的异常实例
 *   可以从 e.getMessage() 拿到具体的错误提示
 *
 * ========== 使用场景示例 ==========
 * Controller 中: throw new RuntimeException("用户名已存在")
 *    → 用户看到的 JSON: { "code": 500, "message": "用户名已存在", "data": null }
 *
 * Service 中: throw new RuntimeException("验证码错误")
 *    → 用户看到的 JSON: { "code": 500, "message": "验证码错误", "data": null }
 *
 * 这样做的优点: 业务代码不需要在每个方法里写 try-catch
 *   异常自然往上抛,由这个类统一兜底,保证返回格式一致
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理所有未捕获的 RuntimeException
     * 将异常信息包装成 Result.error() 返回
     */
    @ExceptionHandler(RuntimeException.class)
    public Result<Void> handleRuntimeException(RuntimeException e) {
        return Result.error(e.getMessage());
    }
}
