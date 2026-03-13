package com.ghlzm.iot.auth.controller;


import com.ghlzm.iot.auth.dto.LoginDTO;
import com.ghlzm.iot.common.response.R;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Author rxbyes
 * Since 2.0
 * Date 2026/3/13 - 14:02
 */
@RestController
public class AuthController {

    @PostMapping("/auth/login")
    public R<?> login(@RequestBody @Valid LoginDTO dto) {
        Map<String, Object> result = new HashMap<>();
        result.put("token", "mock-jwt-token");
        result.put("username", dto.getUsername());
        return R.ok(result);
    }
}

