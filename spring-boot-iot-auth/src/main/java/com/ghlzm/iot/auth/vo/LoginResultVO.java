package com.ghlzm.iot.auth.vo;

import com.ghlzm.iot.system.vo.UserAuthContextVO;
import lombok.Data;

@Data
public class LoginResultVO {

    private String token;

    private String tokenType;

    private Long expiresIn;

    private String tokenHeader;

    private Long userId;

    private String username;

    private String realName;

    private UserAuthContextVO authContext;
}
