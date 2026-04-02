package com.ghlzm.iot.system.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public class UserAuthContextVO {

    private Long userId;

    private Long tenantId;

    private String tenantName;

    private Long orgId;

    private String orgName;

    private String username;

    private String nickname;

    private String realName;

    private String displayName;

    private String phone;

    private String email;

    private String avatar;

    private String accountType;

    private String authStatus;

    private List<String> loginMethods = new ArrayList<>();

    private Date lastLoginTime;

    private String lastLoginIp;

    private String dataScopeType;

    private String dataScopeSummary;

    private boolean superAdmin;

    private String homePath;

    private List<String> roleCodes = new ArrayList<>();

    private List<String> permissions = new ArrayList<>();

    private List<RoleSummaryVO> roles = new ArrayList<>();

    private List<MenuTreeNodeVO> menus = new ArrayList<>();
}
