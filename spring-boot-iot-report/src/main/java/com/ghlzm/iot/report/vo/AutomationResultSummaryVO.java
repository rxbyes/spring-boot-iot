package com.ghlzm.iot.report.vo;

import lombok.Data;

/**
 * 自动化运行汇总统计
 */
@Data
public class AutomationResultSummaryVO {

    private Integer total;
    private Integer passed;
    private Integer failed;
}
