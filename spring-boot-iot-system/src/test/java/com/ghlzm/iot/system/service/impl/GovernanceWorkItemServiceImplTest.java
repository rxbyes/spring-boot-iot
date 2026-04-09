package com.ghlzm.iot.system.service.impl;

import com.ghlzm.iot.system.entity.GovernanceWorkItem;
import com.ghlzm.iot.system.mapper.GovernanceWorkItemMapper;
import com.ghlzm.iot.system.service.model.GovernanceWorkItemCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GovernanceWorkItemServiceImplTest {

    @Mock
    private GovernanceWorkItemMapper workItemMapper;

    @Test
    void openOrRefreshShouldCreateOpenItemForSubject() {
        when(workItemMapper.selectOne(any())).thenReturn(null);
        GovernanceWorkItemServiceImpl service = new GovernanceWorkItemServiceImpl(workItemMapper);

        service.openOrRefresh(new GovernanceWorkItemCommand(
                "PENDING_CONTRACT_RELEASE",
                "PRODUCT",
                1001L,
                1001L,
                null,
                null,
                null,
                null,
                "MODEL_GOVERNANCE",
                "合同尚未发布",
                "{\"publishedRiskMetricCount\":0}",
                "P1",
                10001L
        ));

        verify(workItemMapper).insert(org.mockito.ArgumentMatchers.<GovernanceWorkItem>argThat(item ->
                "PENDING_CONTRACT_RELEASE".equals(item.getWorkItemCode())
                        && "PRODUCT".equals(item.getSubjectType())
                        && Long.valueOf(1001L).equals(item.getSubjectId())
                        && "OPEN".equals(item.getWorkStatus())
                        && "P1".equals(item.getPriorityLevel())
                        && Long.valueOf(10001L).equals(item.getCreateBy())
        ));
    }
}
