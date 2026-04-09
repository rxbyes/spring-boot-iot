package com.ghlzm.iot.system.service.model;

import com.ghlzm.iot.system.enums.DataScopeType;

public record DataPermissionContext(
        Long userId,
        Long tenantId,
        Long orgId,
        DataScopeType dataScopeType,
        boolean superAdmin
) {
}
