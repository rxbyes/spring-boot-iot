package com.ghlzm.iot.report.vo;

import lombok.Data;

/**
 * 自动化结果归档刷新结果。
 */
@Data
public class AutomationResultArchiveRefreshVO {

    private String generatedAt;
    private String latestIndexPath;
    private Integer indexedRuns;
    private Integer skippedFiles;
}
