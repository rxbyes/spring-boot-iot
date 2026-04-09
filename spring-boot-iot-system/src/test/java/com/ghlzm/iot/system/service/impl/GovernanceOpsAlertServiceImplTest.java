package com.ghlzm.iot.system.service.impl;

import com.ghlzm.iot.system.entity.GovernanceOpsAlert;
import com.ghlzm.iot.system.mapper.GovernanceOpsAlertMapper;
import com.ghlzm.iot.system.service.model.GovernanceOpsAlertCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GovernanceOpsAlertServiceImplTest {

    @Mock
    private GovernanceOpsAlertMapper alertMapper;

    @Test
    void raiseOrRefreshShouldReuseExistingAlertCodeAndRefreshLastSeenTime() {
        GovernanceOpsAlert existing = new GovernanceOpsAlert();
        existing.setId(9001L);
        existing.setAlertType("FIELD_DRIFT");
        existing.setAlertCode("product:1001:value");
        existing.setAlertStatus("OPEN");
        when(alertMapper.selectOne(any())).thenReturn(existing);

        GovernanceOpsAlertServiceImpl service = new GovernanceOpsAlertServiceImpl(alertMapper);
        service.raiseOrRefresh(new GovernanceOpsAlertCommand(
                "FIELD_DRIFT",
                "product:1001:value",
                "PRODUCT",
                1001L,
                1001L,
                null,
                null,
                null,
                null,
                null,
                "WARN",
                3L,
                "字段漂移告警",
                "value 已偏离正式合同",
                "product:1001:value",
                "产品1001/value",
                "PAYLOAD_APPLY",
                "{}",
                10001L
        ));

        verify(alertMapper).updateById(org.mockito.ArgumentMatchers.<GovernanceOpsAlert>argThat(alert ->
                Long.valueOf(9001L).equals(alert.getId())
                        && Long.valueOf(3L).equals(alert.getAffectedCount())
                        && "OPEN".equals(alert.getAlertStatus())
                        && "WARN".equals(alert.getSeverityLevel())
        ));
    }
}
