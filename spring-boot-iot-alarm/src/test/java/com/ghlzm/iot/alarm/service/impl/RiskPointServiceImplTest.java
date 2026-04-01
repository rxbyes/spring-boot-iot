package com.ghlzm.iot.alarm.service.impl;

import com.ghlzm.iot.alarm.entity.RiskPoint;
import com.ghlzm.iot.alarm.entity.RiskPointDevice;
import com.ghlzm.iot.alarm.mapper.RiskPointDeviceMapper;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.system.entity.Organization;
import com.ghlzm.iot.system.service.OrganizationService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

class RiskPointServiceImplTest {

    @Test
    void addRiskPointShouldGenerateCodeFromOrganizationAndRiskLevel() {
        RiskPointDeviceMapper deviceMapper = mock(RiskPointDeviceMapper.class);
        OrganizationService organizationService = mock(OrganizationService.class);
        RiskPointServiceImpl service = spy(new RiskPointServiceImpl(deviceMapper, organizationService));

        Organization organization = new Organization();
        organization.setId(7101L);
        organization.setOrgName("ops-center");
        organization.setStatus(1);

        RiskPoint input = new RiskPoint();
        input.setRiskPointName("north-slope");
        input.setOrgId(7101L);
        input.setRiskLevel("critical");

        doReturn(organization).when(organizationService).getById(7101L);
        doReturn(existingRiskPoint("RP-OPSCEN-NORTHS-CRIT-001")).doReturn(null).when(service).getOne(any());
        doAnswer(invocation -> {
            RiskPoint saved = invocation.getArgument(0);
            saved.setId(9001L);
            return true;
        }).when(service).save(any(RiskPoint.class));

        RiskPoint saved = service.addRiskPoint(input);

        assertEquals("ops-center", saved.getOrgName());
        assertEquals("critical", saved.getRiskLevel());
        assertEquals("RP-OPSCEN-NORTHS-CRIT-002", saved.getRiskPointCode());
        assertEquals(0, saved.getDeleted());
    }

    @Test
    void updateRiskPointShouldKeepExistingCodeWhileRefreshingOrganizationName() {
        RiskPointDeviceMapper deviceMapper = mock(RiskPointDeviceMapper.class);
        OrganizationService organizationService = mock(OrganizationService.class);
        RiskPointServiceImpl service = spy(new RiskPointServiceImpl(deviceMapper, organizationService));

        RiskPoint existing = existingRiskPoint("RP-OLD-001");
        existing.setId(12L);
        existing.setOrgId(7101L);
        existing.setOrgName("legacy-org");

        Organization organization = new Organization();
        organization.setId(7102L);
        organization.setOrgName("alarm-team");
        organization.setStatus(1);

        RiskPoint update = new RiskPoint();
        update.setId(12L);
        update.setRiskPointName("north-slope");
        update.setOrgId(7102L);
        update.setRiskLevel("warning");

        doReturn(existing).when(service).getById(12L);
        doReturn(null).when(service).getOne(any());
        doReturn(organization).when(organizationService).getById(7102L);
        doReturn(true).when(service).updateById(any(RiskPoint.class));

        assertDoesNotThrow(() -> service.updateRiskPoint(update));
        assertEquals("RP-OLD-001", update.getRiskPointCode());
        assertEquals("alarm-team", update.getOrgName());
    }

    @Test
    void addRiskPointShouldRejectDisabledOrganization() {
        RiskPointDeviceMapper deviceMapper = mock(RiskPointDeviceMapper.class);
        OrganizationService organizationService = mock(OrganizationService.class);
        RiskPointServiceImpl service = spy(new RiskPointServiceImpl(deviceMapper, organizationService));

        Organization organization = new Organization();
        organization.setId(7101L);
        organization.setOrgName("ops-center");
        organization.setStatus(0);

        RiskPoint input = new RiskPoint();
        input.setRiskPointName("north-slope");
        input.setOrgId(7101L);
        input.setRiskLevel("critical");

        doReturn(organization).when(organizationService).getById(7101L);

        BizException error = assertThrows(BizException.class, () -> service.addRiskPoint(input));
        assertEquals("所属组织已停用", error.getMessage());
    }

    @Test
    void bindDeviceShouldRejectDuplicateMetricBinding() {
        RiskPointDeviceMapper deviceMapper = mock(RiskPointDeviceMapper.class);
        OrganizationService organizationService = mock(OrganizationService.class);
        RiskPointServiceImpl service = spy(new RiskPointServiceImpl(deviceMapper, organizationService));

        RiskPointDevice existing = new RiskPointDevice();
        existing.setRiskPointId(12L);
        existing.setDeviceId(2001L);
        existing.setMetricIdentifier("temperature");
        existing.setDeleted(0);

        RiskPointDevice request = new RiskPointDevice();
        request.setRiskPointId(12L);
        request.setDeviceId(2001L);
        request.setMetricIdentifier("temperature");

        doReturn(existingRiskPoint("RP-OLD-001")).when(service).getById(12L);
        doReturn(existing).when(deviceMapper).selectOne(any());

        BizException error = assertThrows(BizException.class, () -> service.bindDevice(request));
        assertEquals("设备已绑定到该风险点", error.getMessage());
    }

    private RiskPoint existingRiskPoint(String code) {
        RiskPoint riskPoint = new RiskPoint();
        riskPoint.setRiskPointCode(code);
        riskPoint.setDeleted(0);
        return riskPoint;
    }
}
