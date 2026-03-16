package com.ghlzm.iot.framework.security;

/**
 * JWT 解析后的轻量用户主体。
 */
public record JwtUserPrincipal(Long userId, String username) {
}
