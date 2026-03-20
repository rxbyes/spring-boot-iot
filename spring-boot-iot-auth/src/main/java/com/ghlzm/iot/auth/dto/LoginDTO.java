package com.ghlzm.iot.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
/**
 * Author rxbyes
 * Since 2.0
 * Date 2026/3/13 - 14:01
 */
@Data
public class LoginDTO {

    /**
     * 登录方式：account / phone
     */
    private String loginType;

    private String username;

    private String phone;

    @NotBlank
    private String password;
}

