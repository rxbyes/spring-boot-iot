package com.ghlzm.iot.alarm.service.impl;

import com.ghlzm.iot.alarm.entity.RiskMetricCatalog;
import com.ghlzm.iot.alarm.entity.RiskMetricEmergencyPlanBinding;
import com.ghlzm.iot.alarm.entity.RiskMetricLinkageBinding;
import com.ghlzm.iot.alarm.entity.RiskPointDevice;
import com.ghlzm.iot.alarm.entity.RuleDefinition;
import com.ghlzm.iot.alarm.mapper.RiskMetricCatalogMapper;
import com.ghlzm.iot.alarm.mapper.RiskMetricEmergencyPlanBindingMapper;
import com.ghlzm.iot.alarm.mapper.RiskMetricLinkageBindingMapper;
import com.ghlzm.iot.alarm.mapper.RiskPointDeviceMapper;
import com.ghlzm.iot.alarm.mapper.RuleDefinitionMapper;
import com.ghlzm.iot.alarm.service.RiskMetricActionBindingBackfillService;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.entity.ProductContractReleaseBatch;
import com.ghlzm.iot.device.mapper.DeviceMapper;
import com.ghlzm.iot.device.mapper.ProductContractReleaseBatchMapper;
import com.ghlzm.iot.device.mapper.ProductMapper;
import com.ghlzm.iot.system.service.model.GovernanceWorkItemCommand;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RiskGovernanceWorkItemContributorTest {

    @Mock
    private ProductMapper productMapper;

    @Mock
    private ProductContractReleaseBatchMapper productContractReleaseBatchMapper;

    @Mock
    private RiskMetricCatalogMapper riskMetricCatalogMapper;

    @Mock
    private DeviceMapper deviceMapper;

    @Mock
    private RiskPointDeviceMapper riskPointDeviceMapper;

    @Mock
    private RuleDefinitionMapper ruleDefinitionMapper;

    @Mock
    private RiskMetricLinkageBindingMapper linkageBindingMapper;

    @Mock
    private RiskMetricEmergencyPlanBindingMapper emergencyPlanBindingMapper;

    @Mock
    private RiskMetricActionBindingBackfillService backfillService;

    @Test
    void collectWorkItemsShouldProduceAllSixBacklogTaskClasses() {
        Product governed = product(1001L, "phase1-crack", "裂缝产品", LocalDateTime.of(2026, 4, 1, 10, 0));
        Product pending = product(1002L, "phase2-gnss", "GNSS 产品", LocalDateTime.of(2026, 4, 3, 10, 0));
        when(productMapper.selectList(any())).thenReturn(List.of(governed, pending));

        ProductContractReleaseBatch releaseBatch = new ProductContractReleaseBatch();
        releaseBatch.setId(7001L);
        releaseBatch.setProductId(1001L);
        releaseBatch.setCreateTime(LocalDateTime.of(2026, 4, 2, 10, 0));
        when(productContractReleaseBatchMapper.selectList(any())).thenReturn(List.of(releaseBatch));

        when(riskMetricCatalogMapper.selectList(any())).thenReturn(List.of(
                catalog(9101L, 1001L, "value"),
                catalog(9102L, 1001L, "gpsTotalX")
        ));

        Device boundDevice = device(8001L, 1001L, "dev-bound", "已绑定设备");
        Device unboundDevice = device(8002L, 1001L, "dev-unbound", "待绑定设备");
        when(deviceMapper.selectList(any())).thenReturn(List.of(boundDevice, unboundDevice));

        RiskPointDevice coveredBinding = binding(5101L, 8001L, 5001L, 9101L, "value", "裂缝值");
        RiskPointDevice missingPolicyBinding = binding(5102L, 8001L, 5002L, 9102L, "gpsTotalX", "X向累计位移");
        when(riskPointDeviceMapper.selectList(any())).thenReturn(List.of(coveredBinding, missingPolicyBinding));

        RuleDefinition coveredRule = new RuleDefinition();
        coveredRule.setId(6001L);
        coveredRule.setStatus(0);
        coveredRule.setRiskMetricId(9101L);
        coveredRule.setMetricIdentifier("value");
        when(ruleDefinitionMapper.selectList(any())).thenReturn(List.of(coveredRule));

        when(linkageBindingMapper.selectList(any())).thenReturn(List.of(
                linkageBinding(9901L, 9101L, "ACTIVE")
        ));
        when(emergencyPlanBindingMapper.selectList(any())).thenReturn(List.of(
                emergencyPlanBinding(9951L, 9101L, "ACTIVE")
        ));

        RiskGovernanceWorkItemContributor contributor = new RiskGovernanceWorkItemContributor(
                productMapper,
                productContractReleaseBatchMapper,
                riskMetricCatalogMapper,
                deviceMapper,
                riskPointDeviceMapper,
                ruleDefinitionMapper,
                linkageBindingMapper,
                emergencyPlanBindingMapper,
                backfillService
        );

        List<GovernanceWorkItemCommand> commands = contributor.collectWorkItems();

        verify(backfillService).ensureBindingsReadyForRead();
        assertEquals(
                Set.of(
                        "PENDING_PRODUCT_GOVERNANCE",
                        "PENDING_CONTRACT_RELEASE",
                        "PENDING_RISK_BINDING",
                        "PENDING_THRESHOLD_POLICY",
                        "PENDING_LINKAGE_PLAN",
                        "PENDING_REPLAY"
                ),
                commands.stream().map(GovernanceWorkItemCommand::workItemCode).collect(Collectors.toSet())
        );
        assertTrue(commands.stream().anyMatch(command ->
                "PENDING_PRODUCT_GOVERNANCE".equals(command.workItemCode())
                        && Long.valueOf(1002L).equals(command.subjectId())
                        && Long.valueOf(1002L).equals(command.productId())
        ));
        assertTrue(commands.stream().anyMatch(command ->
                "PENDING_RISK_BINDING".equals(command.workItemCode())
                        && Long.valueOf(8002L).equals(command.subjectId())
                        && Long.valueOf(1001L).equals(command.productId())
                        && "dev-unbound".equals(command.snapshotJson() == null ? null : extractDeviceCode(command.snapshotJson()))
        ));
        assertTrue(commands.stream().anyMatch(command ->
                "PENDING_REPLAY".equals(command.workItemCode())
                        && Long.valueOf(9102L).equals(command.riskMetricId())
                        && Long.valueOf(7001L).equals(command.releaseBatchId())
                        && "dev-bound".equals(command.deviceCode())
                        && "phase1-crack".equals(command.productKey())
                        && command.snapshotJson() != null
                        && command.snapshotJson().contains("dimensionKey")
        ));
    }

    @Test
    void collectWorkItemsShouldTolerateReplaySignalsWithoutRiskMetricId() {
        Product product = product(1001L, "phase1-crack", "裂缝产品", LocalDateTime.of(2026, 4, 1, 10, 0));
        when(productMapper.selectList(any())).thenReturn(List.of(product));
        when(productContractReleaseBatchMapper.selectList(any())).thenReturn(List.of());
        when(riskMetricCatalogMapper.selectList(any())).thenReturn(List.of(
                catalog(9101L, 1001L, "value")
        ));
        when(deviceMapper.selectList(any())).thenReturn(List.of(
                device(8001L, 1001L, "dev-bound", "已绑定设备")
        ));
        when(riskPointDeviceMapper.selectList(any())).thenReturn(List.of(
                binding(5103L, 8001L, 5003L, null, "value", "裂缝值")
        ));
        when(ruleDefinitionMapper.selectList(any())).thenReturn(List.of());
        when(linkageBindingMapper.selectList(any())).thenReturn(List.of());
        when(emergencyPlanBindingMapper.selectList(any())).thenReturn(List.of());

        RiskGovernanceWorkItemContributor contributor = new RiskGovernanceWorkItemContributor(
                productMapper,
                productContractReleaseBatchMapper,
                riskMetricCatalogMapper,
                deviceMapper,
                riskPointDeviceMapper,
                ruleDefinitionMapper,
                linkageBindingMapper,
                emergencyPlanBindingMapper,
                backfillService
        );

        List<GovernanceWorkItemCommand> commands = assertDoesNotThrow(contributor::collectWorkItems);

        assertTrue(commands.stream().anyMatch(command ->
                "PENDING_REPLAY".equals(command.workItemCode())
                        && Long.valueOf(1001L).equals(command.productId())
                        && command.riskMetricId() == null
                        && command.snapshotJson() != null
                        && command.snapshotJson().contains("\"metricIdentifier\":\"value\"")
        ));
    }

    private Product product(Long id, String productKey, String productName, LocalDateTime createTime) {
        Product product = new Product();
        product.setId(id);
        product.setProductKey(productKey);
        product.setProductName(productName);
        product.setCreateTime(createTime);
        return product;
    }

    private Device device(Long id, Long productId, String deviceCode, String deviceName) {
        Device device = new Device();
        device.setId(id);
        device.setProductId(productId);
        device.setDeviceCode(deviceCode);
        device.setDeviceName(deviceName);
        device.setLastReportTime(LocalDateTime.of(2026, 4, 9, 9, 0));
        return device;
    }

    private RiskMetricCatalog catalog(Long id, Long productId, String identifier) {
        RiskMetricCatalog catalog = new RiskMetricCatalog();
        catalog.setId(id);
        catalog.setProductId(productId);
        catalog.setContractIdentifier(identifier);
        catalog.setEnabled(1);
        return catalog;
    }

    private RiskPointDevice binding(Long id,
                                    Long deviceId,
                                    Long riskPointId,
                                    Long riskMetricId,
                                    String metricIdentifier,
                                    String metricName) {
        RiskPointDevice binding = new RiskPointDevice();
        binding.setId(id);
        binding.setDeviceId(deviceId);
        binding.setRiskPointId(riskPointId);
        binding.setRiskMetricId(riskMetricId);
        binding.setMetricIdentifier(metricIdentifier);
        binding.setMetricName(metricName);
        return binding;
    }

    private RiskMetricLinkageBinding linkageBinding(Long id, Long riskMetricId, String bindingStatus) {
        RiskMetricLinkageBinding binding = new RiskMetricLinkageBinding();
        binding.setId(id);
        binding.setRiskMetricId(riskMetricId);
        binding.setBindingStatus(bindingStatus);
        binding.setDeleted(0);
        return binding;
    }

    private RiskMetricEmergencyPlanBinding emergencyPlanBinding(Long id, Long riskMetricId, String bindingStatus) {
        RiskMetricEmergencyPlanBinding binding = new RiskMetricEmergencyPlanBinding();
        binding.setId(id);
        binding.setRiskMetricId(riskMetricId);
        binding.setBindingStatus(bindingStatus);
        binding.setDeleted(0);
        return binding;
    }

    private String extractDeviceCode(String snapshotJson) {
        if (snapshotJson == null) {
            return null;
        }
        int marker = snapshotJson.indexOf("\"deviceCode\":\"");
        if (marker < 0) {
            return null;
        }
        int begin = marker + "\"deviceCode\":\"".length();
        int end = snapshotJson.indexOf('"', begin);
        return end > begin ? snapshotJson.substring(begin, end) : null;
    }
}
