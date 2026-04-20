package com.ghlzm.iot.report.service.impl;

import com.ghlzm.iot.device.service.DeviceOnboardingAcceptanceGateway;
import com.ghlzm.iot.device.service.model.DeviceOnboardingAcceptanceLaunch;
import com.ghlzm.iot.device.service.model.DeviceOnboardingAcceptanceProgress;
import com.ghlzm.iot.device.service.model.DeviceOnboardingAcceptanceRequest;
import com.ghlzm.iot.report.service.DeviceOnboardingAcceptanceService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DeviceOnboardingAcceptanceGatewayImpl implements DeviceOnboardingAcceptanceGateway {

    private final DeviceOnboardingAcceptanceService acceptanceService;
    private final Map<String, JobState> jobStates = new ConcurrentHashMap<>();

    @Autowired
    public DeviceOnboardingAcceptanceGatewayImpl(DeviceOnboardingAcceptanceService acceptanceService) {
        this.acceptanceService = acceptanceService;
    }

    @Override
    public DeviceOnboardingAcceptanceLaunch launch(DeviceOnboardingAcceptanceRequest request) {
        String jobId = "doa-" + UUID.randomUUID();
        JobState state = new JobState(jobId);
        jobStates.put(jobId, state);
        submitLaunch(jobId, request);
        return new DeviceOnboardingAcceptanceLaunch(jobId);
    }

    @Override
    public DeviceOnboardingAcceptanceProgress getProgress(String jobId, String runId) {
        if (StringUtils.hasText(runId)) {
            return acceptanceService.loadProgress(runId.trim());
        }
        if (!StringUtils.hasText(jobId)) {
            return null;
        }
        JobState state = jobStates.get(jobId.trim());
        if (state == null) {
            return null;
        }
        if (StringUtils.hasText(state.runId())) {
            return acceptanceService.loadProgress(state.runId());
        }
        return new DeviceOnboardingAcceptanceProgress(
                state.jobId(),
                null,
                state.status(),
                state.summary(),
                state.failedLayers(),
                null
        );
    }

    protected void submitLaunch(String jobId, DeviceOnboardingAcceptanceRequest request) {
        CompletableFuture.runAsync(() -> executeLaunchNow(jobId, request));
    }

    protected void executeLaunchNow(String jobId, DeviceOnboardingAcceptanceRequest request) {
        JobState state = jobStates.get(jobId);
        if (state == null) {
            return;
        }
        try {
            DeviceOnboardingAcceptanceProgress progress = acceptanceService.run(request);
            state.setStatus(progress.status());
            state.setRunId(progress.runId());
            state.setSummary(progress.summary());
            state.setFailedLayers(progress.failedLayers() == null ? List.of() : List.copyOf(progress.failedLayers()));
        } catch (Exception ex) {
            state.setStatus("FAILED");
            state.setSummary(StringUtils.hasText(ex.getMessage()) ? ex.getMessage() : "标准接入验收执行失败");
            state.setFailedLayers(List.of());
        }
    }

    private static final class JobState {

        private final String jobId;
        private String status = "RUNNING";
        private String runId;
        private String summary = "标准接入验收执行中";
        private List<String> failedLayers = List.of();

        private JobState(String jobId) {
            this.jobId = jobId;
        }

        private String jobId() {
            return jobId;
        }

        private String status() {
            return status;
        }

        private void setStatus(String status) {
            this.status = status;
        }

        private String runId() {
            return runId;
        }

        private void setRunId(String runId) {
            this.runId = runId;
        }

        private String summary() {
            return summary;
        }

        private void setSummary(String summary) {
            this.summary = summary;
        }

        private List<String> failedLayers() {
            return failedLayers;
        }

        private void setFailedLayers(List<String> failedLayers) {
            this.failedLayers = failedLayers;
        }
    }
}
