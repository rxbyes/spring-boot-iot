package com.ghlzm.iot.alarm.service.impl;

import com.ghlzm.iot.alarm.dto.RiskGovernanceGapQuery;
import com.ghlzm.iot.alarm.entity.RiskPoint;
import com.ghlzm.iot.alarm.entity.RiskPointDevice;
import com.ghlzm.iot.alarm.entity.RuleDefinition;
import com.ghlzm.iot.alarm.mapper.RiskPointDeviceMapper;
import com.ghlzm.iot.alarm.mapper.RiskPointMapper;
import com.ghlzm.iot.alarm.mapper.RuleDefinitionMapper;
import com.ghlzm.iot.alarm.vo.RiskGovernanceGapItemVO;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.mapper.DeviceMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RiskGovernanceServiceImplTest {

    @Test
    void listMissingBindingsShouldReturnDevicesWithTelemetryButNoRiskPointBinding() {
        DeviceMapper deviceMapper = mock(DeviceMapper.class);
        RiskPointMapper riskPointMapper = mock(RiskPointMapper.class);
        RiskPointDeviceMapper riskPointDeviceMapper = mock(RiskPointDeviceMapper.class);
        RuleDefinitionMapper ruleDefinitionMapper = mock(RuleDefinitionMapper.class);
        RiskGovernanceServiceImpl service = new RiskGovernanceServiceImpl(
                deviceMapper,
                riskPointMapper,
                riskPointDeviceMapper,
                ruleDefinitionMapper
        );

        Device device = new Device();
        device.setId(3002L);
        device.setDeviceCode("demo-device-01");
        device.setDeviceName("演示设备");
        device.setLastReportTime(LocalDateTime.of(2026, 4, 2, 11, 0, 0));

        when(riskPointDeviceMapper.selectList(any())).thenReturn(List.of());
        when(deviceMapper.selectList(any())).thenReturn(List.of(device));

        PageResult<RiskGovernanceGapItemVO> result = service.listMissingBindings(new RiskGovernanceGapQuery());

        assertEquals(1L, result.getTotal());
        assertEquals("demo-device-01", result.getRecords().get(0).getDeviceCode());
        assertEquals("MISSING_BINDING", result.getRecords().get(0).getIssueType());
    }

    @Test
    void listMissingPoliciesShouldReturnBindingsWithoutEnabledRuleDefinitions() {
        DeviceMapper deviceMapper = mock(DeviceMapper.class);
        RiskPointMapper riskPointMapper = mock(RiskPointMapper.class);
        RiskPointDeviceMapper riskPointDeviceMapper = mock(RiskPointDeviceMapper.class);
        RuleDefinitionMapper ruleDefinitionMapper = mock(RuleDefinitionMapper.class);
        RiskGovernanceServiceImpl service = new RiskGovernanceServiceImpl(
                deviceMapper,
                riskPointMapper,
                riskPointDeviceMapper,
                ruleDefinitionMapper
        );

        RiskPointDevice binding = new RiskPointDevice();
        binding.setRiskPointId(8001L);
        binding.setDeviceId(3002L);
        binding.setDeviceCode("demo-device-01");
        binding.setDeviceName("演示设备");
        binding.setMetricIdentifier("dispsX");
        binding.setMetricName("顺滑动方向累计变形量");
        binding.setDeleted(0);

        RiskPoint riskPoint = new RiskPoint();
        riskPoint.setId(8001L);
        riskPoint.setRiskPointName("K79+620边坡");

        RuleDefinition otherMetricRule = new RuleDefinition();
        otherMetricRule.setMetricIdentifier("dispsY");
        otherMetricRule.setStatus(0);
        otherMetricRule.setDeleted(0);

        when(riskPointDeviceMapper.selectList(any())).thenReturn(List.of(binding));
        when(riskPointMapper.selectList(any())).thenReturn(List.of(riskPoint));
        when(ruleDefinitionMapper.selectList(any())).thenReturn(List.of(otherMetricRule));

        PageResult<RiskGovernanceGapItemVO> result = service.listMissingPolicies(new RiskGovernanceGapQuery());

        assertEquals(1L, result.getTotal());
        assertEquals("MISSING_POLICY", result.getRecords().get(0).getIssueType());
        assertEquals("K79+620边坡", result.getRecords().get(0).getRiskPointName());
        assertEquals("dispsX", result.getRecords().get(0).getMetricIdentifier());
    }
}
