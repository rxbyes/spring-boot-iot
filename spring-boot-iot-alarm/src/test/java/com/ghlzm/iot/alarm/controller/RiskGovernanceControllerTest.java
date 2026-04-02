package com.ghlzm.iot.alarm.controller;

import com.ghlzm.iot.alarm.dto.RiskGovernanceGapQuery;
import com.ghlzm.iot.alarm.service.RiskGovernanceService;
import com.ghlzm.iot.alarm.vo.RiskGovernanceGapItemVO;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.common.response.R;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RiskGovernanceControllerTest {

    @Test
    void listMissingBindingsShouldDelegateToService() {
        RiskGovernanceService service = mock(RiskGovernanceService.class);
        RiskGovernanceController controller = new RiskGovernanceController(service);
        RiskGovernanceGapQuery query = new RiskGovernanceGapQuery();
        RiskGovernanceGapItemVO item = new RiskGovernanceGapItemVO();
        item.setIssueType("MISSING_BINDING");
        PageResult<RiskGovernanceGapItemVO> page = PageResult.of(1L, 1L, 10L, List.of(item));
        when(service.listMissingBindings(query)).thenReturn(page);

        R<PageResult<RiskGovernanceGapItemVO>> response = controller.listMissingBindings(query);

        assertEquals(1L, response.getData().getTotal());
        assertEquals("MISSING_BINDING", response.getData().getRecords().get(0).getIssueType());
    }

    @Test
    void listMissingPoliciesShouldDelegateToService() {
        RiskGovernanceService service = mock(RiskGovernanceService.class);
        RiskGovernanceController controller = new RiskGovernanceController(service);
        RiskGovernanceGapQuery query = new RiskGovernanceGapQuery();
        RiskGovernanceGapItemVO item = new RiskGovernanceGapItemVO();
        item.setIssueType("MISSING_POLICY");
        PageResult<RiskGovernanceGapItemVO> page = PageResult.of(1L, 1L, 10L, List.of(item));
        when(service.listMissingPolicies(query)).thenReturn(page);

        R<PageResult<RiskGovernanceGapItemVO>> response = controller.listMissingPolicies(query);

        assertEquals(1L, response.getData().getTotal());
        assertEquals("MISSING_POLICY", response.getData().getRecords().get(0).getIssueType());
    }
}
