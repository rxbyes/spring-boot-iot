package com.ghlzm.iot.system.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class UserAuthContextVO {

    private Long userId;

    private String username;

    private String realName;

    private String displayName;

    private boolean superAdmin;

    private String homePath;

    private List<String> roleCodes = new ArrayList<>();

    private List<String> permissions = new ArrayList<>();

    private List<RoleSummaryVO> roles = new ArrayList<>();

    private List<MenuTreeNodeVO> menus = new ArrayList<>();
}
