package com.ghlzm.iot.system.dto;

import lombok.Data;

@Data
public class UserRoleViewDTO {

    private Long userId;

    private Long roleId;

    private String roleCode;

    private String roleName;
}
