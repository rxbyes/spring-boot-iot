package com.ghlzm.iot.system.vo;

import java.util.List;
import lombok.Data;

/**
 * 治理权限矩阵项。
 */
@Data
public class GovernancePermissionMatrixItemVO {

    private String domainCode;

    private String domainName;

    private String actionCode;

    private String actionName;

    private String operatorPermissionCode;

    private String approverPermissionCode;

    private List<String> defaultRoleCodes;

    private List<String> defaultApproverRoleCodes;

    private Boolean dualControlRequired;

    private String auditModule;
}
