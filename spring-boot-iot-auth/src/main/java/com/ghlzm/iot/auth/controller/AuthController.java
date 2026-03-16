package com.ghlzm.iot.auth.controller;

import com.ghlzm.iot.auth.dto.LoginDTO;
import com.ghlzm.iot.auth.service.AuthService;
import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.framework.security.JwtUserPrincipal;
import com.ghlzm.iot.system.vo.UserAuthContextVO;
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
     * 账号登录，支持用户名密码或手机号密码。
     */
    @PostMapping("/api/auth/login")
    public R<?> login(@RequestBody @Valid LoginDTO dto, HttpServletRequest request) {
        return R.ok(authService.login(dto, request));
    }

    /**
     * 获取当前登录用户主体信息。
     */
    @GetMapping("/api/auth/me")
    public R<UserAuthContextVO> currentUser(Authentication authentication) {
        if (authentication == null) {
            return R.fail(401, "未认证，请先登录");
        }
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof JwtUserPrincipal jwtUserPrincipal)) {
            return R.fail(401, "未认证，请先登录");
        }
        return R.ok(authService.getCurrentUserAuthContext(jwtUserPrincipal.userId()));
    }
}
