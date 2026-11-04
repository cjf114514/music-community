package com.music.aspect;

import com.music.annotation.OperationLogAnnotation;
import com.music.entity.User;
import com.music.repository.UserRepository;
import com.music.service.OperationLogService;
import com.music.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final OperationLogService operationLogService;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    @Around("@annotation(com.music.annotation.OperationLogAnnotation)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result = joinPoint.proceed();

        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            OperationLogAnnotation annotation = method.getAnnotation(OperationLogAnnotation.class);

            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) return result;

            HttpServletRequest request = attributes.getRequest();
            Long userId = (Long) request.getAttribute("adminId");
            if (userId == null) {
                String token = request.getHeader("Authorization");
                if (token != null && token.startsWith("Bearer ")) {
                    token = token.substring(7);
                    userId = jwtUtil.getUserIdFromToken(token);
                }
            }

            String username = null;
            if (userId != null) {
                User user = userRepository.findById(userId).orElse(null);
                if (user != null) {
                    username = user.getNickname() != null ? user.getNickname() : user.getUsername();
                }
            }

            String ip = getClientIp(request);

            Long targetId = extractTargetId(joinPoint);

            operationLogService.log(
                    userId,
                    username,
                    annotation.action(),
                    annotation.module(),
                    annotation.description(),
                    annotation.targetType(),
                    targetId,
                    ip
            );
        } catch (Exception e) {
            // log error silently, don't affect business
        }

        return result;
    }

    private Long extractTargetId(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        for (Object arg : args) {
            if (arg instanceof Long) {
                return (Long) arg;
            }
        }
        return null;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
