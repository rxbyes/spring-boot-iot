package com.ghlzm.iot.admin.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;

/**
 * 前端 history 路由刷新兜底：
 * 浏览器直接访问前端页面时统一回到 SPA 壳层，再由前端路由守卫决定是否跳转登录页。
 */
@Controller
public class SpaForwardController {

    private static final Set<String> RESERVED_FIRST_SEGMENTS = Set.of(
            "api",
            "actuator",
            "swagger-ui",
            "v3",
            "error",
            "assets"
    );

    @GetMapping({
            "/",
            "/{*path}"
    })
    public String forwardToIndex(HttpServletRequest request) {
        if (!shouldForward(request == null ? null : request.getRequestURI())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return "forward:/index.html";
    }

    private boolean shouldForward(String requestUri) {
        if (requestUri == null || "/".equals(requestUri)) {
            return true;
        }
        String normalized = requestUri.startsWith("/") ? requestUri.substring(1) : requestUri;
        if (normalized.isBlank() || normalized.contains(".")) {
            return false;
        }
        int separatorIndex = normalized.indexOf('/');
        String firstSegment = separatorIndex >= 0 ? normalized.substring(0, separatorIndex) : normalized;
        return !RESERVED_FIRST_SEGMENTS.contains(firstSegment);
    }
}
