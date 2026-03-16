package com.ghlzm.iot.auth.controller;

import com.ghlzm.iot.auth.dto.LoginDTO;
import com.ghlzm.iot.auth.service.AuthService;
import com.ghlzm.iot.common.response.R;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口控制器。
 */
@RestController
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 用户名密码登录。
     */
    @PostMapping("/api/auth/login")
    public R<?> login(@RequestBody @Valid LoginDTO dto, HttpServletRequest request) {
        return R.ok(authService.login(dto, request));
    }

    /**
     * 获取当前登录用户主体信息。
     */
    @GetMapping("/api/auth/me")
    public R<?> currentUser(Authentication authentication) {
        if (authentication == null) {
            return R.fail(401, "未认证，请先登录");
        }
        return R.ok(authentication.getPrincipal());
    }
}
