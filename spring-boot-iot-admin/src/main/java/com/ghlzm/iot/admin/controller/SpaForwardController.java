package com.ghlzm.iot.admin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 前端 history 路由刷新兜底：
 * 浏览器直接访问前端页面时统一回到 SPA 壳层，再由前端路由守卫决定是否跳转登录页。
 */
@Controller
public class SpaForwardController {

    @GetMapping({
            "/",
            "/{path:^(?!api$|message$|actuator$|swagger-ui$|v3$|error$|doc\\.html$)[^.]+}"
    })
    public String forwardToIndex() {
        return "forward:/index.html";
    }
}
