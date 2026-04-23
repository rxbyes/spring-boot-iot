package com.ghlzm.iot.device.service.impl;

import com.ghlzm.iot.device.dto.DeviceOnboardingSuggestionQuery;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.mapper.ProductMapper;
import com.ghlzm.iot.device.service.PublishedProductContractSnapshotService;
import com.ghlzm.iot.device.service.UnregisteredDeviceRosterService;
import com.ghlzm.iot.device.service.model.PublishedProductContractSnapshot;
import com.ghlzm.iot.device.vo.DeviceOnboardingSuggestionVO;
import com.ghlzm.iot.device.vo.DevicePageVO;
import com.ghlzm.iot.framework.config.IotProperties;
import com.ghlzm.iot.framework.protocol.ProtocolSecurityDefinitionProvider;
import com.ghlzm.iot.framework.protocol.template.entity.ProtocolTemplateDefinitionSnapshot;
import com.ghlzm.iot.framework.protocol.template.mapper.ProtocolTemplateDefinitionSnapshotMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceOnboardingSuggestionServiceImplTest {

    @Mock
    private UnregisteredDeviceRosterService rosterService;
    @Mock
    private ProductMapper productMapper;
    @Mock
    private ProtocolSecurityDefinitionProvider protocolSecurityDefinitionProvider;
    @Mock
    private ProtocolTemplateDefinitionSnapshotMapper protocolTemplateDefinitionSnapshotMapper;
    @Mock
    private PublishedProductContractSnapshotService publishedProductContractSnapshotService;

    private DeviceOnboardingSuggestionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DeviceOnboardingSuggestionServiceImpl(
                rosterService,
                productMapper,
                protocolSecurityDefinitionProvider,
                protocolTemplateDefinitionSnapshotMapper,
                publishedProductContractSnapshotService
        );
    }

    @Test
    void shouldSuggestProductFamilyTemplateAndRuleForUnregisteredDevice() {
        DevicePageVO candidate = new DevicePageVO();
        candidate.setDeviceCode("south-rtu-01");
        candidate.setDeviceName("未登记设备");
        candidate.setProductKey("south_rtu");
        candidate.setProtocolCode("mqtt-json");
        candidate.setLastTraceId("trace-unregistered-001");
        candidate.setLastPayload("{\"deviceCode\":\"south-rtu-01\"}");

        Product product = new Product();
        product.setId(1001L);
        product.setProductKey("south_rtu");
        product.setProductName("南方 RTU");

        IotProperties.Protocol.FamilyDefinition familyDefinition = new IotProperties.Protocol.FamilyDefinition();
        familyDefinition.setFamilyCode("legacy-dp");
        familyDefinition.setDisplayName("Legacy DP");
        familyDefinition.setProtocolCode("mqtt-json");
        familyDefinition.setDecryptProfileCode("des-62000001");

        ProtocolTemplateDefinitionSnapshot templateSnapshot = new ProtocolTemplateDefinitionSnapshot();
        templateSnapshot.setId(9301L);
        templateSnapshot.setTemplateId(9301L);
        templateSnapshot.setTemplateCode("legacy-dp-crack-v1");
        templateSnapshot.setFamilyCode("legacy-dp");
        templateSnapshot.setProtocolCode("mqtt-json");
        templateSnapshot.setPublishedVersionNo(1);
        templateSnapshot.setLifecycleStatus("PUBLISHED");

        Map<String, IotProperties.Protocol.FamilyDefinition> families = new LinkedHashMap<>();
        families.put("legacy-dp", familyDefinition);

        when(rosterService.findByTraceId(null, "trace-unregistered-001")).thenReturn(candidate);
        when(productMapper.selectOne(any())).thenReturn(product);
        when(protocolSecurityDefinitionProvider.listFamilyDefinitions()).thenReturn(families);
        when(protocolTemplateDefinitionSnapshotMapper.selectList(any())).thenReturn(List.of(templateSnapshot));
        when(publishedProductContractSnapshotService.getRequiredSnapshot(1001L))
                .thenReturn(PublishedProductContractSnapshot.empty(1001L));

        DeviceOnboardingSuggestionVO result = service.suggest(
                null,
                new DeviceOnboardingSuggestionQuery("trace-unregistered-001")
        );

        assertEquals("south_rtu", result.getRecommendedProductKey());
        assertEquals("legacy-dp", result.getRecommendedFamilyCode());
        assertEquals("legacy-dp-crack-v1", result.getRecommendedTemplateCode());
        assertFalse(result.getRuleGaps().isEmpty());
    }
}
