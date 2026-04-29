package com.ghlzm.iot.system.vo;

import lombok.Data;

@Data
public class ObservabilityMessageArchiveBatchCompareSourceVO {

    private String confirmReportPath;
    private String resolvedDryRunJsonPath;
    private String resolvedApplyJsonPath;
    private Boolean dryRunAvailable;
    private Boolean applyAvailable;
}
