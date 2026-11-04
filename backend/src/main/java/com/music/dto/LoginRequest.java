package com.music.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
//登录请求DTO
//登录请求DTO 是用户登录时,前端提交的登录请求参数,包含用户名、密码、验证码、验证码标识
@Data
//前端 → 后端：用 XXXRequest
//后端 → 前端：用 XXXResponse
public class LoginRequest {
    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    @NotBlank(message = "验证码不能为空")
    private String captchaCode;

    @NotBlank(message = "验证码标识不能为空")
    private String captchaKey;
}
