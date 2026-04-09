package com.ghlzm.iot.device.service.impl;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.device.dto.DeviceRelationUpsertDTO;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.DeviceRelation;
import com.ghlzm.iot.device.mapper.DeviceMapper;
import com.ghlzm.iot.device.mapper.DeviceRelationMapper;
import com.ghlzm.iot.device.mapper.ProductMapper;
import com.ghlzm.iot.device.service.model.DeviceRelationRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceRelationServiceImplTest {

    @Mock
    private DeviceRelationMapper deviceRelationMapper;
    @Mock
    private DeviceMapper deviceMapper;
    @Mock
    private ProductMapper productMapper;

    private DeviceRelationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DeviceRelationServiceImpl(deviceRelationMapper, deviceMapper, productMapper);
    }

    @Test
    void createRelationShouldRejectDuplicateLogicalChannelWithinSameParent() {
        DeviceRelationUpsertDTO dto = new DeviceRelationUpsertDTO();
        dto.setParentDeviceCode("SK00EA0D1307986");
        dto.setLogicalChannelCode("L1_LF_1");
        dto.setChildDeviceCode("202018143");
        dto.setRelationType("collector_child");
        dto.setCanonicalizationStrategy("LF_VALUE");
        dto.setStatusMirrorStrategy("SENSOR_STATE");

        when(deviceMapper.selectOne(any())).thenReturn(
                device(10L, "SK00EA0D1307986", 1001L),
                device(20L, "202018143", 2002L)
        );
        when(deviceRelationMapper.selectOne(any())).thenReturn(existingRelation(9001L, "SK00EA0D1307986", "L1_LF_1"));

        BizException ex = assertThrows(BizException.class, () -> service.createRelation(1L, dto));

        assertEquals("同一父设备下逻辑通道已存在: L1_LF_1", ex.getMessage());
    }

    @Test
    void listRulesByParentDeviceCodeShouldReturnEnabledRulesOrderedByLogicalChannel() {
        when(deviceRelationMapper.selectList(any())).thenReturn(List.of(
                relation(2L, "SK00EA0D1307986", "L1_LF_2", "202018135", 2002L),
                relation(1L, "SK00EA0D1307986", "L1_LF_1", "202018143", 2002L)
        ));

        List<DeviceRelationRule> rules = service.listEnabledRulesByParentDeviceCode("SK00EA0D1307986");

        assertEquals(List.of("L1_LF_1", "L1_LF_2"), rules.stream().map(DeviceRelationRule::getLogicalChannelCode).toList());
        assertEquals(List.of("202018143", "202018135"), rules.stream().map(DeviceRelationRule::getChildDeviceCode).toList());
        assertEquals(List.of("LF_VALUE", "LF_VALUE"), rules.stream().map(DeviceRelationRule::getCanonicalizationStrategy).toList());
    }

    private Device device(Long id, String deviceCode, Long productId) {
        Device device = new Device();
        device.setId(id);
        device.setTenantId(1L);
        device.setProductId(productId);
        device.setDeviceCode(deviceCode);
        device.setDeleted(0);
        return device;
    }

    private DeviceRelation existingRelation(Long id, String parentDeviceCode, String logicalChannelCode) {
        return relation(id, parentDeviceCode, logicalChannelCode, "202018143", 2002L);
    }

    private DeviceRelation relation(Long id,
                                    String parentDeviceCode,
                                    String logicalChannelCode,
                                    String childDeviceCode,
                                    Long childProductId) {
        DeviceRelation relation = new DeviceRelation();
        relation.setId(id);
        relation.setTenantId(1L);
        relation.setParentDeviceId(10L);
        relation.setParentDeviceCode(parentDeviceCode);
        relation.setLogicalChannelCode(logicalChannelCode);
        relation.setChildDeviceId(20L);
        relation.setChildDeviceCode(childDeviceCode);
        relation.setChildProductId(childProductId);
        relation.setChildProductKey("south-crack-sensor-v1");
        relation.setRelationType("collector_child");
        relation.setCanonicalizationStrategy("LF_VALUE");
        relation.setStatusMirrorStrategy("SENSOR_STATE");
        relation.setEnabled(1);
        relation.setDeleted(0);
        return relation;
    }
}
