package com.ghlzm.iot.framework.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

/**
 * JWT 认证过滤器。
 * 作用：从请求头提取 token，解析后写入 SecurityContext，供后续鉴权链使用。
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;

    public JwtAuthenticationFilter(JwtTokenService jwtTokenService) {
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);
        if (!StringUtils.hasText(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        Optional<JwtUserPrincipal> principal = jwtTokenService.parseToken(token);
        if (principal.isEmpty()) {
            // 无效 token 不中断请求，由后续鉴权入口统一返回 401。
            SecurityContextHolder.clearContext();
            filterChain.doFilter(request, response);
            return;
        }

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            JwtUserPrincipal jwtPrincipal = principal.get();
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(jwtPrincipal, null, Collections.emptyList());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        filterChain.doFilter(request, response);
    }

    /**
     * 从 Authorization 头中提取真实 token（去掉 Bearer 前缀）。
     */
    private String resolveToken(HttpServletRequest request) {
        String headerValue = request.getHeader(jwtTokenService.getTokenHeader());
        if (!StringUtils.hasText(headerValue)) {
            return null;
        }
        String expectedPrefix = jwtTokenService.getTokenPrefix() + " ";
        if (headerValue.startsWith(expectedPrefix)) {
            return headerValue.substring(expectedPrefix.length()).trim();
        }
        return null;
    }
}
