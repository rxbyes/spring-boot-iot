package com.ghlzm.iot.system.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRoleBindingDTO {

    private Long id;

    private Long userId;

    private Long roleId;
}
