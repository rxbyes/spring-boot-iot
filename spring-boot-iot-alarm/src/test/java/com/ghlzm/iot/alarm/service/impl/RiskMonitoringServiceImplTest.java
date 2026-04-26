package com.ghlzm.iot.alarm.service.impl;

import com.ghlzm.iot.alarm.auto.RiskRuntimeLevelResolver;
import com.ghlzm.iot.alarm.dto.RiskMonitoringListQuery;
import com.ghlzm.iot.alarm.entity.AlarmRecord;
import com.ghlzm.iot.alarm.entity.RiskPoint;
import com.ghlzm.iot.alarm.entity.RiskPointDevice;
import com.ghlzm.iot.alarm.mapper.AlarmRecordMapper;
import com.ghlzm.iot.alarm.mapper.EventRecordMapper;
import com.ghlzm.iot.alarm.mapper.RiskPointDeviceMapper;
import com.ghlzm.iot.alarm.mapper.RiskPointMapper;
import com.ghlzm.iot.alarm.vo.RiskMonitoringGisPointVO;
import com.ghlzm.iot.alarm.vo.RiskMonitoringDetailVO;
import com.ghlzm.iot.alarm.vo.RiskMonitoringListItemVO;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.DeviceProperty;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.mapper.DeviceMapper;
import com.ghlzm.iot.device.mapper.DeviceMessageLogMapper;
import com.ghlzm.iot.device.mapper.DevicePropertyMapper;
import com.ghlzm.iot.device.mapper.ProductMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RiskMonitoringServiceImplTest {

    @Mock
    private RiskPointMapper riskPointMapper;
    @Mock
    private RiskPointDeviceMapper riskPointDeviceMapper;
    @Mock
    private AlarmRecordMapper alarmRecordMapper;
    @Mock
    private EventRecordMapper eventRecordMapper;
    @Mock
    private DeviceMapper deviceMapper;
    @Mock
    private DevicePropertyMapper devicePropertyMapper;
    @Mock
    private DeviceMessageLogMapper deviceMessageLogMapper;
    @Mock
    private ProductMapper productMapper;
    @Mock
    private JdbcTemplate jdbcTemplate;

    private RiskMonitoringServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RiskMonitoringServiceImpl(
                riskPointMapper,
                riskPointDeviceMapper,
                alarmRecordMapper,
                eventRecordMapper,
                deviceMapper,
                devicePropertyMapper,
                deviceMessageLogMapper,
                productMapper,
                jdbcTemplate,
                new RiskRuntimeLevelResolver()
        );
        when(jdbcTemplate.queryForObject(any(String.class), eq(Integer.class), eq("risk_point_device"))).thenReturn(1);
        when(jdbcTemplate.queryForList(any(String.class), eq(String.class), eq("risk_point_device"))).thenReturn(List.of(
                "id",
                "risk_point_id",
                "device_id",
                "device_code",
                "device_name",
                "metric_identifier",
                "metric_name",
                "default_threshold",
                "threshold_unit",
                "create_time",
                "update_time",
                "create_by",
                "update_by",
                "deleted"
        ));
    }

    @Test
    void listRealtimeItemsShouldPreferActiveAlarmLevelOverArchivedRiskPointLevel() {
        RiskPoint riskPoint = riskPoint();
        riskPoint.setRiskPointLevel("level_1");
        riskPoint.setCurrentRiskLevel("blue");
        riskPoint.setRiskLevel("blue");
        RiskPointDevice binding = binding();
        AlarmRecord activeAlarm = activeAlarm("critical");

        when(riskPointDeviceMapper.selectList(any())).thenReturn(List.of(binding));
        when(riskPointMapper.selectList(any())).thenReturn(List.of(riskPoint));
        when(deviceMapper.selectBatchIds(any())).thenReturn(List.of(device()));
        when(productMapper.selectBatchIds(any())).thenReturn(List.of(product()));
        when(devicePropertyMapper.selectList(any())).thenReturn(List.of(property()));
        when(alarmRecordMapper.selectList(any())).thenReturn(List.of(activeAlarm), List.of(activeAlarm));
        when(eventRecordMapper.selectList(any())).thenReturn(List.of());

        PageResult<RiskMonitoringListItemVO> page = service.listRealtimeItems(new RiskMonitoringListQuery());

        assertEquals(1L, page.getTotal());
        assertEquals("red", page.getRecords().get(0).getRiskLevel());
        assertEquals("red", page.getRecords().get(0).getCurrentRiskLevel());
        assertEquals("level_1", page.getRecords().get(0).getRiskPointLevel());
    }

    @Test
    void listGisPointsShouldPreferRuntimeAlarmLevelOverArchivedRiskPointLevel() {
        RiskPoint riskPoint = riskPoint();
        riskPoint.setRiskPointLevel("level_2");
        riskPoint.setCurrentRiskLevel("blue");
        riskPoint.setRiskLevel("blue");
        RiskPointDevice binding = binding();
        AlarmRecord activeAlarm = activeAlarm("critical");

        when(riskPointMapper.selectList(any())).thenReturn(List.of(riskPoint));
        when(riskPointDeviceMapper.selectList(any())).thenReturn(List.of(binding));
        when(deviceMapper.selectBatchIds(any())).thenReturn(List.of(device()));
        when(alarmRecordMapper.selectList(any())).thenReturn(List.of(activeAlarm), List.of(activeAlarm));
        when(eventRecordMapper.selectList(any())).thenReturn(List.of());

        List<RiskMonitoringGisPointVO> points = service.listGisPoints(null);

        assertEquals(1, points.size());
        assertEquals("red", points.get(0).getRiskLevel());
        assertEquals("red", points.get(0).getCurrentRiskLevel());
        assertEquals("level_2", points.get(0).getRiskPointLevel());
    }

    @Test
    void getRealtimeDetailShouldExposeArchiveAndRuntimeLevelsSeparately() {
        RiskPoint riskPoint = riskPoint();
        riskPoint.setRiskPointLevel("level_3");
        riskPoint.setCurrentRiskLevel("blue");
        riskPoint.setRiskLevel("blue");
        RiskPointDevice binding = binding();
        AlarmRecord activeAlarm = activeAlarm("critical");

        when(riskPointDeviceMapper.selectById(9901L)).thenReturn(binding);
        when(riskPointMapper.selectList(any())).thenReturn(List.of(riskPoint));
        when(deviceMapper.selectBatchIds(any())).thenReturn(List.of(device()));
        when(productMapper.selectBatchIds(any())).thenReturn(List.of(product()));
        when(devicePropertyMapper.selectList(any())).thenReturn(List.of(property()));
        when(alarmRecordMapper.selectList(any())).thenReturn(List.of(activeAlarm), List.of(activeAlarm), List.of(activeAlarm));
        when(eventRecordMapper.selectList(any())).thenReturn(List.of(), List.of());
        when(deviceMessageLogMapper.selectList(any())).thenReturn(List.of());

        RiskMonitoringDetailVO detail = service.getRealtimeDetail(9901L);

        assertEquals("level_3", detail.getRiskPointLevel());
        assertEquals("red", detail.getCurrentRiskLevel());
        assertEquals("red", detail.getRiskLevel());
    }

    @Test
    void listRealtimeItemsShouldIgnoreActiveAlarmFromDifferentRiskPointForSameDeviceMetric() {
        RiskPoint riskPoint = riskPoint();
        riskPoint.setRiskPointLevel("level_1");
        riskPoint.setCurrentRiskLevel("blue");
        riskPoint.setRiskLevel("blue");
        RiskPointDevice binding = binding();
        AlarmRecord activeAlarm = activeAlarm("critical");
        activeAlarm.setRiskPointId(9009L);

        when(riskPointDeviceMapper.selectList(any())).thenReturn(List.of(binding));
        when(riskPointMapper.selectList(any())).thenReturn(List.of(riskPoint));
        when(deviceMapper.selectBatchIds(any())).thenReturn(List.of(device()));
        when(productMapper.selectBatchIds(any())).thenReturn(List.of(product()));
        when(devicePropertyMapper.selectList(any())).thenReturn(List.of(property()));
        when(alarmRecordMapper.selectList(any())).thenReturn(List.of(activeAlarm), List.of(activeAlarm));
        when(eventRecordMapper.selectList(any())).thenReturn(List.of());

        PageResult<RiskMonitoringListItemVO> page = service.listRealtimeItems(new RiskMonitoringListQuery());

        assertEquals(1L, page.getTotal());
        assertEquals("blue", page.getRecords().get(0).getRiskLevel());
        assertEquals("blue", page.getRecords().get(0).getCurrentRiskLevel());
    }

    private RiskPoint riskPoint() {
        RiskPoint riskPoint = new RiskPoint();
        riskPoint.setId(8001L);
        riskPoint.setRiskPointName("K79+620边坡");
        riskPoint.setRegionId(7001L);
        riskPoint.setRegionName("测试区域");
        riskPoint.setDeleted(0);
        return riskPoint;
    }

    private RiskPointDevice binding() {
        RiskPointDevice binding = new RiskPointDevice();
        binding.setId(9901L);
        binding.setRiskPointId(8001L);
        binding.setDeviceId(3002L);
        binding.setDeviceCode("84330701");
        binding.setDeviceName("子设备-84330701");
        binding.setMetricIdentifier("dispsX");
        binding.setMetricName("顺滑动方向累计变形量");
        binding.setThresholdUnit("mm");
        binding.setDeleted(0);
        return binding;
    }

    private Device device() {
        Device device = new Device();
        device.setId(3002L);
        device.setProductId(1001L);
        device.setDeviceCode("84330701");
        device.setDeviceName("子设备-84330701");
        device.setOnlineStatus(1);
        device.setLastReportTime(LocalDateTime.of(2026, 4, 2, 10, 0, 0));
        return device;
    }

    private Product product() {
        Product product = new Product();
        product.setId(1001L);
        product.setProductName("深部位移仪");
        return product;
    }

    private DeviceProperty property() {
        DeviceProperty property = new DeviceProperty();
        property.setDeviceId(3002L);
        property.setIdentifier("dispsX");
        property.setPropertyValue("12.8");
        property.setReportTime(LocalDateTime.of(2026, 4, 2, 10, 0, 0));
        return property;
    }

    private AlarmRecord activeAlarm(String alarmLevel) {
        AlarmRecord alarm = new AlarmRecord();
        alarm.setId(8801L);
        alarm.setRiskPointId(8001L);
        alarm.setDeviceId(3002L);
        alarm.setMetricName("顺滑动方向累计变形量");
        alarm.setAlarmLevel(alarmLevel);
        alarm.setStatus(0);
        alarm.setCreateTime(LocalDateTime.of(2026, 4, 2, 10, 5, 0));
        alarm.setDeleted(0);
        return alarm;
    }
}
