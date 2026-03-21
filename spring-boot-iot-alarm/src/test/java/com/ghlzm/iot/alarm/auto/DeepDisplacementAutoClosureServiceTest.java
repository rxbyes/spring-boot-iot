package com.ghlzm.iot.alarm.auto;

import com.ghlzm.iot.alarm.entity.AlarmRecord;
import com.ghlzm.iot.alarm.entity.EmergencyPlan;
import com.ghlzm.iot.alarm.entity.EventRecord;
import com.ghlzm.iot.alarm.entity.LinkageRule;
import com.ghlzm.iot.alarm.entity.RiskPoint;
import com.ghlzm.iot.alarm.entity.RiskPointDevice;
import com.ghlzm.iot.alarm.mapper.EmergencyPlanMapper;
import com.ghlzm.iot.alarm.mapper.LinkageRuleMapper;
import com.ghlzm.iot.alarm.mapper.RiskPointDeviceMapper;
import com.ghlzm.iot.alarm.mapper.RiskPointMapper;
import com.ghlzm.iot.alarm.service.AlarmRecordService;
import com.ghlzm.iot.alarm.service.EventRecordService;
import com.ghlzm.iot.device.entity.DeviceProperty;
import com.ghlzm.iot.device.event.DeviceRiskEvaluationEvent;
import com.ghlzm.iot.device.mapper.DevicePropertyMapper;
import com.ghlzm.iot.framework.config.IotProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeepDisplacementAutoClosureServiceTest {

    @Mock
    private AlarmRecordService alarmRecordService;
    @Mock
    private EventRecordService eventRecordService;
    @Mock
    private RiskPointMapper riskPointMapper;
    @Mock
    private RiskPointDeviceMapper riskPointDeviceMapper;
    @Mock
    private LinkageRuleMapper linkageRuleMapper;
    @Mock
    private EmergencyPlanMapper emergencyPlanMapper;
    @Mock
    private DevicePropertyMapper devicePropertyMapper;

    private DeepDisplacementAutoClosureService service;

    @BeforeEach
    void setUp() {
        IotProperties properties = new IotProperties();
        IotProperties.Alarm alarm = new IotProperties.Alarm();
        IotProperties.Alarm.AutoClosure autoClosure = new IotProperties.Alarm.AutoClosure();
        autoClosure.setEnabled(true);
        autoClosure.setCooldownMinutes(30);
        alarm.setAutoClosure(autoClosure);
        properties.setAlarm(alarm);
        service = new DeepDisplacementAutoClosureService(
                alarmRecordService,
                eventRecordService,
                riskPointMapper,
                riskPointDeviceMapper,
                linkageRuleMapper,
                emergencyPlanMapper,
                devicePropertyMapper,
                properties,
                null
        );
    }

    @Test
    void processShouldCreateAlarmEventAndDispatchForRed() {
        RiskPoint riskPoint = buildRiskPoint(8001L, "info", 88L);
        RiskPointDevice binding = buildBinding(8001L, 3002L, "84330701", "dispsX", "顺滑动方向累计变形量");
        LinkageRule linkageRule = new LinkageRule();
        linkageRule.setId(8301L);
        linkageRule.setRuleName("深部位移红色联动");
        linkageRule.setTriggerCondition("{\"metric\":\"dispsX\",\"op\":\">=\",\"threshold\":20}");
        EmergencyPlan emergencyPlan = new EmergencyPlan();
        emergencyPlan.setId(8401L);
        emergencyPlan.setPlanName("边坡深部位移红色应急预案");
        emergencyPlan.setRiskLevel("critical");
        emergencyPlan.setDescription("边坡深部位移达到红色阈值时执行");

        mockCommonLookups(riskPoint, binding, buildProperty(binding, "25.6"));
        when(linkageRuleMapper.selectList(any())).thenReturn(List.of(linkageRule));
        when(emergencyPlanMapper.selectList(any())).thenReturn(List.of(emergencyPlan));
        when(alarmRecordService.getOne(any())).thenReturn(null);
        when(alarmRecordService.addAlarm(any())).thenAnswer(invocation -> {
            AlarmRecord alarmRecord = invocation.getArgument(0);
            alarmRecord.setId(8503L);
            return alarmRecord;
        });
        when(eventRecordService.addEvent(any())).thenAnswer(invocation -> {
            EventRecord eventRecord = invocation.getArgument(0);
            eventRecord.setId(8601L);
            return eventRecord;
        });

        service.process(buildEvent("84330701", Map.of("dispsX", 25.6)));

        ArgumentCaptor<AlarmRecord> alarmCaptor = ArgumentCaptor.forClass(AlarmRecord.class);
        verify(alarmRecordService).addAlarm(alarmCaptor.capture());
        assertEquals("critical", alarmCaptor.getValue().getAlarmLevel());
        assertEquals(">= 20 mm", alarmCaptor.getValue().getThresholdValue());
        assertTrue(alarmCaptor.getValue().getRemark().contains("\"metricIdentifier\":\"dispsX\""));
        assertTrue(alarmCaptor.getValue().getRemark().contains("\"traceId\":\"trace-red-001\""));

        ArgumentCaptor<EventRecord> eventCaptor = ArgumentCaptor.forClass(EventRecord.class);
        verify(eventRecordService).addEvent(eventCaptor.capture());
        assertEquals("critical", eventCaptor.getValue().getRiskLevel());
        assertEquals(88L, eventCaptor.getValue().getResponsibleUser());
        assertTrue(eventCaptor.getValue().getReviewNotes().contains("\"name\":\"边坡深部位移红色应急预案\""));

        verify(eventRecordService).dispatchEvent(8601L, 88L, 88L);

        ArgumentCaptor<RiskPoint> riskPointCaptor = ArgumentCaptor.forClass(RiskPoint.class);
        verify(riskPointMapper).updateById(riskPointCaptor.capture());
        assertEquals("critical", riskPointCaptor.getValue().getRiskLevel());
    }

    @Test
    void processShouldPreferSceneMatchedEmergencyPlan() {
        RiskPoint riskPoint = buildRiskPoint(8001L, "info", 88L);
        RiskPointDevice binding = buildBinding(8001L, 3002L, "84330701", "dispsX", "顺滑动方向累计变形量");
        EmergencyPlan genericPlan = new EmergencyPlan();
        genericPlan.setId(8401L);
        genericPlan.setPlanName("锅炉超温应急预案");
        genericPlan.setRiskLevel("critical");
        genericPlan.setDescription("锅炉区域出现超温告警时执行");
        EmergencyPlan scopedPlan = new EmergencyPlan();
        scopedPlan.setId(8402L);
        scopedPlan.setPlanName("边坡深部位移红色应急预案");
        scopedPlan.setRiskLevel("critical");
        scopedPlan.setDescription("边坡深部位移达到红色阈值时执行");

        mockCommonLookups(riskPoint, binding, buildProperty(binding, "25.6"));
        when(linkageRuleMapper.selectList(any())).thenReturn(List.of());
        when(emergencyPlanMapper.selectList(any())).thenReturn(List.of(genericPlan, scopedPlan));
        when(alarmRecordService.getOne(any())).thenReturn(null);
        when(alarmRecordService.addAlarm(any())).thenAnswer(invocation -> {
            AlarmRecord alarmRecord = invocation.getArgument(0);
            alarmRecord.setId(8503L);
            return alarmRecord;
        });
        when(eventRecordService.addEvent(any())).thenAnswer(invocation -> {
            EventRecord eventRecord = invocation.getArgument(0);
            eventRecord.setId(8601L);
            return eventRecord;
        });

        service.process(buildEvent("84330701", Map.of("dispsX", 25.6)));

        ArgumentCaptor<EventRecord> eventCaptor = ArgumentCaptor.forClass(EventRecord.class);
        verify(eventRecordService).addEvent(eventCaptor.capture());
        assertTrue(eventCaptor.getValue().getReviewNotes().contains("\"id\":8402"));
        assertTrue(eventCaptor.getValue().getReviewNotes().contains("边坡深部位移红色应急预案"));
    }

    @Test
    void processShouldLeaveEmergencyPlanEmptyWhenOnlyGenericPlanExists() {
        RiskPoint riskPoint = buildRiskPoint(8001L, "info", 88L);
        RiskPointDevice binding = buildBinding(8001L, 3002L, "84330701", "dispsX", "顺滑动方向累计变形量");
        EmergencyPlan genericPlan = new EmergencyPlan();
        genericPlan.setId(8401L);
        genericPlan.setPlanName("锅炉超温应急预案");
        genericPlan.setRiskLevel("critical");
        genericPlan.setDescription("锅炉区域出现超温告警时执行");

        mockCommonLookups(riskPoint, binding, buildProperty(binding, "25.6"));
        when(linkageRuleMapper.selectList(any())).thenReturn(List.of());
        when(emergencyPlanMapper.selectList(any())).thenReturn(List.of(genericPlan));
        when(alarmRecordService.getOne(any())).thenReturn(null);
        when(alarmRecordService.addAlarm(any())).thenAnswer(invocation -> {
            AlarmRecord alarmRecord = invocation.getArgument(0);
            alarmRecord.setId(8503L);
            return alarmRecord;
        });
        when(eventRecordService.addEvent(any())).thenAnswer(invocation -> {
            EventRecord eventRecord = invocation.getArgument(0);
            eventRecord.setId(8601L);
            return eventRecord;
        });

        service.process(buildEvent("84330701", Map.of("dispsX", 25.6)));

        ArgumentCaptor<AlarmRecord> alarmCaptor = ArgumentCaptor.forClass(AlarmRecord.class);
        verify(alarmRecordService).addAlarm(alarmCaptor.capture());
        assertTrue(alarmCaptor.getValue().getRemark().contains("\"planId\":null"));

        ArgumentCaptor<EventRecord> eventCaptor = ArgumentCaptor.forClass(EventRecord.class);
        verify(eventRecordService).addEvent(eventCaptor.capture());
        assertTrue(eventCaptor.getValue().getReviewNotes().contains("\"emergencyPlan\":null"));
    }

    @Test
    void processShouldNotSelectPlanWithOnlyContextButNoSceneKeywords() {
        RiskPoint riskPoint = buildRiskPoint(8001L, "info", 88L);
        RiskPointDevice binding = buildBinding(8001L, 3002L, "84330701", "dispsX", "顺滑动方向累计变形量");
        EmergencyPlan contextOnlyPlan = new EmergencyPlan();
        contextOnlyPlan.setId(8410L);
        contextOnlyPlan.setPlanName("RP_SK00FB0D1310195_SW 现场处置预案");
        contextOnlyPlan.setRiskLevel("critical");
        contextOnlyPlan.setDescription("适用于该监测点异常时执行");

        mockCommonLookups(riskPoint, binding, buildProperty(binding, "25.6"));
        when(linkageRuleMapper.selectList(any())).thenReturn(List.of());
        when(emergencyPlanMapper.selectList(any())).thenReturn(List.of(contextOnlyPlan));
        when(alarmRecordService.getOne(any())).thenReturn(null);
        when(alarmRecordService.addAlarm(any())).thenAnswer(invocation -> {
            AlarmRecord alarmRecord = invocation.getArgument(0);
            alarmRecord.setId(8503L);
            return alarmRecord;
        });
        when(eventRecordService.addEvent(any())).thenAnswer(invocation -> {
            EventRecord eventRecord = invocation.getArgument(0);
            eventRecord.setId(8601L);
            return eventRecord;
        });

        service.process(buildEvent("84330701", Map.of("dispsX", 25.6)));

        ArgumentCaptor<AlarmRecord> alarmCaptor = ArgumentCaptor.forClass(AlarmRecord.class);
        verify(alarmRecordService).addAlarm(alarmCaptor.capture());
        assertTrue(alarmCaptor.getValue().getRemark().contains("\"planId\":null"));

        ArgumentCaptor<EventRecord> eventCaptor = ArgumentCaptor.forClass(EventRecord.class);
        verify(eventRecordService).addEvent(eventCaptor.capture());
        assertTrue(eventCaptor.getValue().getReviewNotes().contains("\"emergencyPlan\":null"));
    }

    @Test
    void processShouldSkipDuplicateAlarmWithinCooldown() {
        RiskPoint riskPoint = buildRiskPoint(8001L, "info", 88L);
        RiskPointDevice binding = buildBinding(8001L, 3002L, "84330701", "dispsX", "顺滑动方向累计变形量");

        mockCommonLookups(riskPoint, binding, buildProperty(binding, "25.6"));
        when(alarmRecordService.getOne(any())).thenReturn(new AlarmRecord());

        service.process(buildEvent("84330701", Map.of("dispsX", 25.6)));

        verify(alarmRecordService, never()).addAlarm(any());
        verify(eventRecordService, never()).addEvent(any());
        verify(eventRecordService, never()).dispatchEvent(any(), any(), any());
    }

    @Test
    void processShouldCreateAlarmOnlyForYellow() {
        RiskPoint riskPoint = buildRiskPoint(8001L, "info", null);
        RiskPointDevice binding = buildBinding(8001L, 3002L, "84330701", "dispsX", "顺滑动方向累计变形量");

        mockCommonLookups(riskPoint, binding, buildProperty(binding, "7.5"));
        when(linkageRuleMapper.selectList(any())).thenReturn(List.of());
        when(emergencyPlanMapper.selectList(any())).thenReturn(List.of());
        when(alarmRecordService.getOne(any())).thenReturn(null);

        service.process(buildEvent("84330701", Map.of("dispsX", 7.5)));

        ArgumentCaptor<AlarmRecord> alarmCaptor = ArgumentCaptor.forClass(AlarmRecord.class);
        verify(alarmRecordService).addAlarm(alarmCaptor.capture());
        assertEquals("medium", alarmCaptor.getValue().getAlarmLevel());
        verify(eventRecordService, never()).addEvent(any());
        verify(eventRecordService, never()).dispatchEvent(any(), any(), any());

        ArgumentCaptor<RiskPoint> riskPointCaptor = ArgumentCaptor.forClass(RiskPoint.class);
        verify(riskPointMapper).updateById(riskPointCaptor.capture());
        assertEquals("warning", riskPointCaptor.getValue().getRiskLevel());
    }

    @Test
    void processShouldKeepRiskPointAtHighestBoundSeverity() {
        RiskPoint riskPoint = buildRiskPoint(8001L, "warning", null);
        RiskPointDevice blueBinding = buildBinding(8001L, 3002L, "84330701", "dispsX", "顺滑动方向累计变形量");
        RiskPointDevice redBinding = buildBinding(8001L, 3003L, "84330695", "dispsY", "垂直坡面方向累计变形量");

        when(riskPointDeviceMapper.selectList(any())).thenReturn(List.of(blueBinding), List.of(blueBinding, redBinding));
        when(riskPointMapper.selectList(any())).thenReturn(List.of(riskPoint));
        when(riskPointMapper.selectById(8001L)).thenReturn(riskPoint);
        when(devicePropertyMapper.selectOne(any())).thenReturn(buildProperty(blueBinding, "1.2"), buildProperty(redBinding, "22.8"));

        service.process(buildEvent("84330701", Map.of("dispsX", 1.2)));

        verify(alarmRecordService, never()).addAlarm(any());
        verify(eventRecordService, never()).addEvent(any());

        ArgumentCaptor<RiskPoint> riskPointCaptor = ArgumentCaptor.forClass(RiskPoint.class);
        verify(riskPointMapper).updateById(riskPointCaptor.capture());
        assertEquals("critical", riskPointCaptor.getValue().getRiskLevel());
    }

    private void mockCommonLookups(RiskPoint riskPoint, RiskPointDevice binding, DeviceProperty property) {
        when(riskPointDeviceMapper.selectList(any())).thenReturn(List.of(binding), List.of(binding));
        when(riskPointMapper.selectList(any())).thenReturn(List.of(riskPoint));
        when(riskPointMapper.selectById(riskPoint.getId())).thenReturn(riskPoint);
        when(devicePropertyMapper.selectOne(any())).thenReturn(property);
    }

    private DeviceRiskEvaluationEvent buildEvent(String deviceCode, Map<String, Object> properties) {
        return new DeviceRiskEvaluationEvent(
                1L,
                3002L,
                deviceCode,
                "子设备-" + deviceCode,
                1001L,
                "demo-product",
                "mqtt-json",
                "property",
                "$dp",
                "trace-red-001",
                LocalDateTime.of(2026, 3, 21, 9, 30, 0),
                properties
        );
    }

    private RiskPoint buildRiskPoint(Long id, String riskLevel, Long responsibleUser) {
        RiskPoint riskPoint = new RiskPoint();
        riskPoint.setId(id);
        riskPoint.setTenantId(1L);
        riskPoint.setRiskPointName("K79+620边坡");
        riskPoint.setRegionId(7001L);
        riskPoint.setRegionName("测试区域");
        riskPoint.setRiskLevel(riskLevel);
        riskPoint.setResponsibleUser(responsibleUser);
        riskPoint.setStatus(0);
        riskPoint.setDeleted(0);
        return riskPoint;
    }

    private RiskPointDevice buildBinding(Long riskPointId, Long deviceId, String deviceCode, String metricIdentifier, String metricName) {
        RiskPointDevice binding = new RiskPointDevice();
        binding.setRiskPointId(riskPointId);
        binding.setDeviceId(deviceId);
        binding.setDeviceCode(deviceCode);
        binding.setDeviceName("子设备-" + deviceCode);
        binding.setMetricIdentifier(metricIdentifier);
        binding.setMetricName(metricName);
        binding.setDeleted(0);
        return binding;
    }

    private DeviceProperty buildProperty(RiskPointDevice binding, String value) {
        DeviceProperty property = new DeviceProperty();
        property.setDeviceId(binding.getDeviceId());
        property.setIdentifier(binding.getMetricIdentifier());
        property.setPropertyValue(new BigDecimal(value).stripTrailingZeros().toPlainString());
        return property;
    }
}
