package com.ghlzm.iot.admin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 前端 history 路由刷新兜底：
 * 浏览器直接访问前端页面时统一回到 SPA 壳层，再由前端路由守卫决定是否跳转登录页。
 */
@Controller
public class SpaForwardController {

    private static final String FRONTEND_FIRST_SEGMENT_PATTERN =
            "^(?!api$|actuator$|swagger-ui$|v3$|error$|assets$|doc\\\\.html$)[^.]+";

    @GetMapping({
            "/",
            "/{path:" + FRONTEND_FIRST_SEGMENT_PATTERN + "}",
            "/{path:" + FRONTEND_FIRST_SEGMENT_PATTERN + "}/**/{subPath:[^.]+}"
    })
    public String forwardToIndex() {
        return "forward:/index.html";
    }
}
