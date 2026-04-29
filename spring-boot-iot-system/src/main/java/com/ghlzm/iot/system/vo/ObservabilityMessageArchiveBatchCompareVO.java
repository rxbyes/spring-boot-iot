package com.ghlzm.iot.system.vo;

import java.util.List;
import lombok.Data;

@Data
public class ObservabilityMessageArchiveBatchCompareVO {

    private String batchNo;
    private String sourceTable;
    private String status;
    private String compareStatus;
    private String compareMessage;
    private ObservabilityMessageArchiveBatchCompareSourceVO sources;
    private ObservabilityMessageArchiveBatchCompareSummaryVO summaryCompare;
    private List<ObservabilityMessageArchiveBatchCompareTableVO> tableComparisons;
}
