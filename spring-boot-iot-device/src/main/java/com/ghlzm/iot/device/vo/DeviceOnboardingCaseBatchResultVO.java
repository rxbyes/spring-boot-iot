package com.ghlzm.iot.device.vo;

import java.util.List;
import lombok.Data;

/**
 * 批量接入案例执行结果。
 */
@Data
public class DeviceOnboardingCaseBatchResultVO {

    private String action;

    private Integer requestedCount;

    private Integer successCount;

    private Integer failedCount;

    private List<SuccessItem> successItems;

    private List<FailureItem> failureItems;

    private List<FailureGroup> failureGroups;

    @Data
    public static class SuccessItem {
        private Long caseId;
        private String caseCode;
        private String caseName;
        private String currentStep;
        private String status;
        private String deviceCode;
        private String acceptanceStatus;
        private String acceptanceRunId;
    }

    @Data
    public static class FailureItem {
        private Long caseId;
        private String caseCode;
        private String caseName;
        private String failureKey;
        private String message;
    }

    @Data
    public static class FailureGroup {
        private String failureKey;
        private String summary;
        private Integer count;
        private List<String> caseCodes;
    }
}
