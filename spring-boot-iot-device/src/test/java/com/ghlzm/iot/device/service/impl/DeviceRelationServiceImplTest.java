package com.ghlzm.iot.device.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.device.dto.DeviceRelationUpsertDTO;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.DeviceRelation;
import com.ghlzm.iot.device.mapper.DeviceMapper;
import com.ghlzm.iot.device.mapper.DeviceRelationMapper;
import com.ghlzm.iot.device.mapper.ProductMapper;
import com.ghlzm.iot.device.service.model.DeviceRelationRule;
import com.ghlzm.iot.device.vo.DeviceRelationVO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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
        initializeTableInfo();
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
    void createRelationShouldRejectDuplicateLogicalChannelWithinSameParentCodeEvenWhenLegacyRowUsesDifferentParentDeviceId() {
        DeviceRelationUpsertDTO dto = new DeviceRelationUpsertDTO();
        dto.setParentDeviceCode("SK00FB0D1310195");
        dto.setLogicalChannelCode("L1_SW_1");
        dto.setChildDeviceCode("84330701");
        dto.setRelationType("collector_child");
        dto.setCanonicalizationStrategy("LEGACY");
        dto.setStatusMirrorStrategy("SENSOR_STATE");

        when(deviceMapper.selectOne(any())).thenReturn(
                device(10L, "SK00FB0D1310195", 1001L),
                device(20L, "84330701", 2002L)
        );
        when(deviceRelationMapper.selectOne(any())).thenAnswer(invocation -> {
            AbstractWrapper<?, ?, ?> wrapper = invocation.getArgument(0);
            String sqlSegment = wrapper.getSqlSegment();
            return sqlSegment.contains("parent_device_code")
                    ? existingRelation(9002L, "SK00FB0D1310195", "L1_SW_1")
                    : null;
        });

        BizException ex = assertThrows(BizException.class, () -> service.createRelation(1L, dto));

        assertEquals("同一父设备下逻辑通道已存在: L1_SW_1", ex.getMessage());
    }

    @Test
    void listByParentDeviceCodeShouldDeduplicateDuplicateLogicalChannelsAndKeepMostRecentlyUpdatedRelation() {
        when(deviceRelationMapper.selectList(any())).thenReturn(List.of(
                relation(202604110500055L,
                        "SK00FB0D1310195",
                        "L1_SW_1",
                        "84330701",
                        2002L,
                        LocalDateTime.of(2026, 4, 12, 10, 38, 25),
                        LocalDateTime.of(2026, 4, 11, 18, 12, 35)),
                relation(2040980828752973825L,
                        "SK00FB0D1310195",
                        "L1_SW_1",
                        "84330702",
                        2002L,
                        LocalDateTime.of(2026, 4, 10, 10, 13, 53),
                        LocalDateTime.of(2026, 4, 6, 10, 32, 4)),
                relation(202604110500056L,
                        "SK00FB0D1310195",
                        "L1_SW_2",
                        "84330695",
                        2002L,
                        LocalDateTime.of(2026, 4, 12, 10, 38, 26),
                        LocalDateTime.of(2026, 4, 11, 18, 12, 35))
        ));

        List<DeviceRelationVO> relations = service.listByParentDeviceCode(1001L, "SK00FB0D1310195");

        assertEquals(List.of("L1_SW_1", "L1_SW_2"), relations.stream().map(DeviceRelationVO::getLogicalChannelCode).toList());
        assertEquals(List.of("84330701", "84330695"), relations.stream().map(DeviceRelationVO::getChildDeviceCode).toList());
        assertEquals(List.of(202604110500055L, 202604110500056L), relations.stream().map(DeviceRelationVO::getId).toList());
    }

    @Test
    void listRulesByParentDeviceCodeShouldDeduplicateDuplicateLogicalChannelsAndKeepLatestRule() {
        when(deviceRelationMapper.selectList(any())).thenReturn(List.of(
                relation(2L, "SK00EA0D1307986", "L1_LF_1", "202018143", 2002L),
                relation(6L, "SK00EA0D1307986", "L1_LF_1", "202018999", 2002L),
                relation(4L, "SK00EA0D1307986", "L1_LF_2", "202018135", 2002L)
        ));

        List<DeviceRelationRule> rules = service.listEnabledRulesByParentDeviceCode("SK00EA0D1307986");

        assertEquals(List.of("L1_LF_1", "L1_LF_2"), rules.stream().map(DeviceRelationRule::getLogicalChannelCode).toList());
        assertEquals(List.of("202018999", "202018135"), rules.stream().map(DeviceRelationRule::getChildDeviceCode).toList());
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
        return relation(id,
                parentDeviceCode,
                logicalChannelCode,
                "202018143",
                2002L,
                LocalDateTime.of(2026, 4, 12, 10, 38, 25),
                LocalDateTime.of(2026, 4, 11, 18, 12, 35));
    }

    private DeviceRelation relation(Long id,
                                    String parentDeviceCode,
                                    String logicalChannelCode,
                                    String childDeviceCode,
                                    Long childProductId) {
        return relation(id,
                parentDeviceCode,
                logicalChannelCode,
                childDeviceCode,
                childProductId,
                LocalDateTime.of(2026, 4, 12, 10, 38, 25),
                LocalDateTime.of(2026, 4, 11, 18, 12, 35));
    }

    private DeviceRelation relation(Long id,
                                    String parentDeviceCode,
                                    String logicalChannelCode,
                                    String childDeviceCode,
                                    Long childProductId,
                                    LocalDateTime updateTime,
                                    LocalDateTime createTime) {
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
        relation.setCreateTime(createTime);
        relation.setUpdateTime(updateTime);
        return relation;
    }

    private void initializeTableInfo() {
        if (TableInfoHelper.getTableInfo(DeviceRelation.class) != null) {
            return;
        }
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), "deviceRelationServiceImplTest"),
                DeviceRelation.class
        );
    }
}
