package com.ghlzm.iot.report.service.impl;

import com.ghlzm.iot.alarm.service.RiskMetricCatalogService;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.DeviceMessageLog;
import com.ghlzm.iot.device.entity.ProductContractReleaseBatch;
import com.ghlzm.iot.device.mapper.ProductContractReleaseBatchMapper;
import com.ghlzm.iot.device.mapper.ProductContractReleaseSnapshotMapper;
import com.ghlzm.iot.device.mapper.ProductMetricResolverSnapshotMapper;
import com.ghlzm.iot.device.service.CollectorChildInsightService;
import com.ghlzm.iot.device.service.DeviceMessageService;
import com.ghlzm.iot.device.service.DeviceService;
import com.ghlzm.iot.device.service.PublishedProductContractSnapshotService;
import com.ghlzm.iot.device.service.model.DeviceOnboardingAcceptanceProgress;
import com.ghlzm.iot.device.service.model.DeviceOnboardingAcceptanceRequest;
import com.ghlzm.iot.device.service.model.PublishedProductContractSnapshot;
import com.ghlzm.iot.framework.config.IotProperties;
import com.ghlzm.iot.framework.protocol.ProtocolSecurityDefinitionProvider;
import com.ghlzm.iot.framework.protocol.template.entity.ProtocolTemplateDefinitionSnapshot;
import com.ghlzm.iot.framework.protocol.template.mapper.ProtocolTemplateDefinitionSnapshotMapper;
import com.ghlzm.iot.framework.protocol.template.service.ProtocolTemplateReplayService;
import com.ghlzm.iot.framework.protocol.template.vo.ProtocolTemplateReplayVO;
import com.ghlzm.iot.telemetry.service.TelemetryQueryService;
import com.ghlzm.iot.telemetry.service.dto.TelemetryHistoryBatchResponse;
import com.ghlzm.iot.telemetry.service.dto.TelemetryHistoryBatchSeries;
import com.ghlzm.iot.telemetry.service.dto.TelemetryHistoryBucketPoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceOnboardingAcceptanceServiceImplTest {

    @TempDir
    Path tempDir;

    @Mock
    private ProtocolSecurityDefinitionProvider protocolSecurityDefinitionProvider;

    @Mock
    private ProtocolTemplateDefinitionSnapshotMapper protocolTemplateDefinitionSnapshotMapper;

    @Mock
    private ProtocolTemplateReplayService protocolTemplateReplayService;

    @Mock
    private PublishedProductContractSnapshotService publishedProductContractSnapshotService;

    @Mock
    private ProductContractReleaseBatchMapper productContractReleaseBatchMapper;

    @Mock
    private ProductContractReleaseSnapshotMapper productContractReleaseSnapshotMapper;

    @Mock
    private ProductMetricResolverSnapshotMapper productMetricResolverSnapshotMapper;

    @Mock
    private DeviceService deviceService;

    @Mock
    private DeviceMessageService deviceMessageService;

    @Mock
    private TelemetryQueryService telemetryQueryService;

    @Mock
    private CollectorChildInsightService collectorChildInsightService;

    @Mock
    private RiskMetricCatalogService riskMetricCatalogService;

    @Test
    void runShouldWriteRegistryArtifactsAndFlagReadSideFailure() throws Exception {
        DeviceOnboardingAcceptanceRequest request = new DeviceOnboardingAcceptanceRequest(
                9101L,
                1L,
                "CASE-9101",
                "裂缝传感器接入",
                1001L,
                88001L,
                "DEV-9101",
                "legacy-dp-crack",
                "aes-62000002",
                "nf-crack-v1"
        );

        IotProperties.Protocol.FamilyDefinition family = new IotProperties.Protocol.FamilyDefinition();
        family.setFamilyCode("legacy-dp-crack");
        family.setDecryptProfileCode("aes-62000002");
        when(protocolSecurityDefinitionProvider.getFamilyDefinition("legacy-dp-crack")).thenReturn(family);

        IotProperties.Protocol.DecryptProfile profile = new IotProperties.Protocol.DecryptProfile();
        profile.setProfileCode("aes-62000002");
        when(protocolSecurityDefinitionProvider.getDecryptProfile("aes-62000002")).thenReturn(profile);

        ProtocolTemplateDefinitionSnapshot snapshot = new ProtocolTemplateDefinitionSnapshot();
        snapshot.setTemplateCode("nf-crack-v1");
        snapshot.setLifecycleStatus("PUBLISHED");
        when(protocolTemplateDefinitionSnapshotMapper.selectList(any())).thenReturn(List.of(snapshot));

        ProtocolTemplateReplayVO replay = new ProtocolTemplateReplayVO();
        replay.setMatched(Boolean.TRUE);
        replay.setSummary("模板回放成功");
        when(protocolTemplateReplayService.replay(any())).thenReturn(replay);

        when(publishedProductContractSnapshotService.getRequiredSnapshot(1001L)).thenReturn(
                PublishedProductContractSnapshot.builder()
                        .productId(1001L)
                        .releaseBatchId(88001L)
                        .publishedIdentifier("value")
                        .build()
        );

        ProductContractReleaseBatch batch = new ProductContractReleaseBatch();
        batch.setId(88001L);
        batch.setProductId(1001L);
        batch.setReleaseStatus("PUBLISHED");
        when(productContractReleaseBatchMapper.selectById(88001L)).thenReturn(batch);
        when(productContractReleaseSnapshotMapper.selectCount(any())).thenReturn(1L);
        when(productMetricResolverSnapshotMapper.selectCount(any())).thenReturn(1L);

        Device device = new Device();
        device.setId(101L);
        device.setProductId(1001L);
        device.setDeviceCode("DEV-9101");
        device.setNodeType(1);
        when(deviceService.getRequiredByCode("DEV-9101")).thenReturn(device);

        DeviceMessageLog messageLog = new DeviceMessageLog();
        messageLog.setPayload("{\"L1_LF_1\":{\"2026-04-18T18:00:00Z\":0.2136}}");
        messageLog.setReportTime(LocalDateTime.now());
        when(deviceMessageService.listMessageLogs("DEV-9101")).thenReturn(List.of(messageLog));

        when(collectorChildInsightService.listRecommendedMetrics(1001L)).thenReturn(List.of("value"));
        when(telemetryQueryService.getLatest(101L)).thenReturn(Map.of());
        when(telemetryQueryService.getHistoryBatch(any())).thenReturn(new TelemetryHistoryBatchResponse());
        when(riskMetricCatalogService.listEnabledByProduct(1001L)).thenReturn(List.of());

        DeviceOnboardingAcceptanceServiceImpl service = new DeviceOnboardingAcceptanceServiceImpl(
                tempDir,
                protocolSecurityDefinitionProvider,
                protocolTemplateDefinitionSnapshotMapper,
                protocolTemplateReplayService,
                publishedProductContractSnapshotService,
                productContractReleaseBatchMapper,
                productContractReleaseSnapshotMapper,
                productMetricResolverSnapshotMapper,
                deviceService,
                deviceMessageService,
                telemetryQueryService,
                collectorChildInsightService,
                riskMetricCatalogService
        );

        DeviceOnboardingAcceptanceProgress progress = service.run(request);

        assertThat(progress.status()).isEqualTo("FAILED");
        assertThat(progress.runId()).isNotBlank();
        assertThat(progress.failedLayers()).contains("读侧层");
        assertThat(progress.jumpPath()).isEqualTo("/automation-results?runId=" + progress.runId());
        assertThat(Files.readString(tempDir.resolve("logs").resolve("acceptance").resolve("registry-run-" + progress.runId() + ".json")))
                .contains("device-onboarding.read-side")
                .contains("\"status\" : \"failed\"");
    }

    @Test
    void loadProgressShouldReadWrittenRunLedger() throws Exception {
        String runId = "20260418200500";
        Path logsDir = Files.createDirectories(tempDir.resolve("logs").resolve("acceptance"));
        Files.writeString(
                logsDir.resolve("registry-run-" + runId + ".json"),
                """
                        {
                          "runId": "20260418200500",
                          "options": {
                            "caseId": 9101,
                            "caseCode": "CASE-9101",
                            "workflow": "device-onboarding"
                          },
                          "summary": {
                            "total": 8,
                            "passed": 8,
                            "failed": 0
                          },
                          "results": [
                            {
                              "scenarioId": "device-onboarding.protocol-family",
                              "runnerType": "deviceOnboardingCheck",
                              "status": "passed",
                              "blocking": "blocker",
                              "summary": "协议族命中",
                              "evidenceFiles": [],
                              "details": {
                                "layer": "协议层"
                              }
                            }
                          ]
                        }
                        """
        );

        DeviceOnboardingAcceptanceServiceImpl service = new DeviceOnboardingAcceptanceServiceImpl(
                tempDir,
                protocolSecurityDefinitionProvider,
                protocolTemplateDefinitionSnapshotMapper,
                protocolTemplateReplayService,
                publishedProductContractSnapshotService,
                productContractReleaseBatchMapper,
                productContractReleaseSnapshotMapper,
                productMetricResolverSnapshotMapper,
                deviceService,
                deviceMessageService,
                telemetryQueryService,
                collectorChildInsightService,
                riskMetricCatalogService
        );

        DeviceOnboardingAcceptanceProgress progress = service.loadProgress(runId);

        assertThat(progress.status()).isEqualTo("PASSED");
        assertThat(progress.runId()).isEqualTo(runId);
        assertThat(progress.failedLayers()).isEmpty();
        assertThat(progress.summary()).contains("8/8");
    }
}
