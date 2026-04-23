package com.ghlzm.iot.report.service.impl;

import com.ghlzm.iot.device.service.model.DeviceOnboardingAcceptanceLaunch;
import com.ghlzm.iot.device.service.model.DeviceOnboardingAcceptanceProgress;
import com.ghlzm.iot.device.service.model.DeviceOnboardingAcceptanceRequest;
import com.ghlzm.iot.report.service.DeviceOnboardingAcceptanceService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceOnboardingAcceptanceGatewayImplTest {

    @Mock
    private DeviceOnboardingAcceptanceService acceptanceService;

    @Test
    void launchShouldReturnJobIdAndExposeCompletedRunProgress() {
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
        DeviceOnboardingAcceptanceProgress finished = new DeviceOnboardingAcceptanceProgress(
                null,
                "20260418193000",
                "PASSED",
                "8/8 检查项通过",
                List.of(),
                "/automation-results?runId=20260418193000"
        );
        when(acceptanceService.run(request)).thenReturn(finished);
        when(acceptanceService.loadProgress("20260418193000")).thenReturn(finished);

        TestableGateway gateway = new TestableGateway(acceptanceService);

        DeviceOnboardingAcceptanceLaunch launch = gateway.launch(request);
        DeviceOnboardingAcceptanceProgress progress = gateway.getProgress(launch.jobId(), null);

        assertThat(launch.jobId()).startsWith("doa-");
        assertThat(progress.runId()).isEqualTo("20260418193000");
        assertThat(progress.status()).isEqualTo("PASSED");
        assertThat(progress.jumpPath()).isEqualTo("/automation-results?runId=20260418193000");
    }

    @Test
    void getProgressShouldReadRunLedgerWhenRunIdProvided() {
        DeviceOnboardingAcceptanceProgress finished = new DeviceOnboardingAcceptanceProgress(
                null,
                "20260418193000",
                "PASSED",
                "8/8 检查项通过",
                List.of(),
                "/automation-results?runId=20260418193000"
        );
        when(acceptanceService.loadProgress("20260418193000")).thenReturn(finished);

        DeviceOnboardingAcceptanceGatewayImpl gateway = new DeviceOnboardingAcceptanceGatewayImpl(acceptanceService);

        DeviceOnboardingAcceptanceProgress progress = gateway.getProgress(null, "20260418193000");

        assertThat(progress.status()).isEqualTo("PASSED");
        verify(acceptanceService).loadProgress("20260418193000");
    }

    private static final class TestableGateway extends DeviceOnboardingAcceptanceGatewayImpl {

        private TestableGateway(DeviceOnboardingAcceptanceService acceptanceService) {
            super(acceptanceService);
        }

        @Override
        protected void submitLaunch(String jobId, DeviceOnboardingAcceptanceRequest request) {
            executeLaunchNow(jobId, request);
        }
    }
}
