package com.ghlzm.iot.system.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleMenuBindingDTO {

    private Long id;

    private Long roleId;

    private Long menuId;
}
