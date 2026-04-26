package com.ghlzm.iot.system.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ObservabilityMessageArchiveBatchVO {

    private Long id;
    private String batchNo;
    private String sourceTable;
    private String governanceMode;
    private String status;
    private Integer retentionDays;
    private LocalDateTime cutoffAt;
    private String confirmReportPath;
    private LocalDateTime confirmReportGeneratedAt;
    private Integer confirmedExpiredRows;
    private Integer candidateRows;
    private Integer archivedRows;
    private Integer deletedRows;
    private String failedReason;
    private String artifactsJson;
    private String compareStatus;
    private String compareStatusLabel;
    private Long deltaConfirmedVsDeleted;
    private Long deltaDryRunVsDeleted;
    private Long remainingExpiredRows;
    private Boolean previewAvailable;
    private String previewReasonCode;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
