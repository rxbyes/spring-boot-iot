package com.ghlzm.iot.device.vo;

import lombok.Data;

import java.util.List;

@Data
public class DeviceOnboardingBatchResultVO {

    private Integer requestedCount;

    private Integer activatedCount;

    private Integer rejectedCount;

    private List<String> activatedTraceIds;

    private List<String> activatedDeviceCodes;

    private List<ErrorItem> errors;

    @Data
    public static class ErrorItem {
        private String traceId;
        private String deviceCode;
        private String message;
    }
}
