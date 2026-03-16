package com.ghlzm.iot.auth.service.impl;

import com.ghlzm.iot.auth.dto.LoginDTO;
import com.ghlzm.iot.auth.service.AuthService;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.framework.security.JwtTokenService;
import com.ghlzm.iot.system.entity.User;
import com.ghlzm.iot.system.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 认证服务实现。
 */
@Service
public class AuthServiceImpl implements AuthService {

    /**
     * 历史初始化脚本中的占位密码。
     * 兼容策略：当数据库仍是该占位符时，允许默认密码 123456 登录，避免老环境切换中断。
     */
    private static final String LEGACY_PLACEHOLDER_PASSWORD = "replace_with_bcrypt_password";

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    public AuthServiceImpl(UserService userService,
                           PasswordEncoder passwordEncoder,
                           JwtTokenService jwtTokenService) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    public Map<String, Object> login(LoginDTO dto, HttpServletRequest request) {
        User user = userService.getByUsername(dto.getUsername());
        if (user == null || !isPasswordMatched(dto.getPassword(), user.getPassword())) {
            throw new BizException(401, "用户名或密码错误");
        }

        if (!Integer.valueOf(1).equals(user.getStatus())) {
            throw new BizException(403, "用户已禁用");
        }

        // 登录成功后刷新审计字段，便于追踪最近一次登录来源。
        user.setLastLoginTime(new Date());
        user.setLastLoginIp(resolveClientIp(request));
        userService.updateById(user);

        String token = jwtTokenService.createToken(user.getId(), user.getUsername());

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("tokenType", jwtTokenService.getTokenPrefix());
        result.put("expiresIn", jwtTokenService.getTokenExpireSeconds());
        result.put("tokenHeader", jwtTokenService.getTokenHeader());
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("realName", user.getRealName());
        return result;
    }

    /**
     * 密码匹配策略：
     * 1. 占位符 -> 固定密码兼容；
     * 2. bcrypt 前缀 -> BCrypt 校验；
     * 3. 其余 -> 明文兼容（历史数据）。
     */
    private boolean isPasswordMatched(String rawPassword, String storedPassword) {
        if (!StringUtils.hasText(rawPassword) || !StringUtils.hasText(storedPassword)) {
            return false;
        }

        if (storedPassword.contains(LEGACY_PLACEHOLDER_PASSWORD)) {
            return "123456".equals(rawPassword);
        }

        if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$") || storedPassword.startsWith("$2y$")) {
            return passwordEncoder.matches(rawPassword, storedPassword);
        }

        return rawPassword.equals(storedPassword);
    }

    /**
     * 解析客户端 IP，优先读取反向代理头。
     */
    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            int index = forwardedFor.indexOf(',');
            return index >= 0 ? forwardedFor.substring(0, index).trim() : forwardedFor.trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(realIp)) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}
