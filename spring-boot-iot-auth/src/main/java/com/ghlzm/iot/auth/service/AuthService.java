package com.ghlzm.iot.auth.service;

import com.ghlzm.iot.auth.dto.LoginDTO;
import com.ghlzm.iot.auth.vo.LoginResultVO;
import com.ghlzm.iot.system.vo.UserAuthContextVO;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 登录认证服务。
 */
public interface AuthService {

    /**
     * 用户名密码登录并返回令牌信息。
     */
    LoginResultVO login(LoginDTO dto, HttpServletRequest request);

    /**
     * 查询当前登录用户权限上下文。
     */
    UserAuthContextVO getCurrentUserAuthContext(Long userId);
}
