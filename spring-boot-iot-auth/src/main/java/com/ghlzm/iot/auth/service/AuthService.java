package com.ghlzm.iot.auth.service;

import com.ghlzm.iot.auth.dto.LoginDTO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

/**
 * 登录认证服务。
 */
public interface AuthService {

    /**
     * 用户名密码登录并返回令牌信息。
     */
    Map<String, Object> login(LoginDTO dto, HttpServletRequest request);
}
