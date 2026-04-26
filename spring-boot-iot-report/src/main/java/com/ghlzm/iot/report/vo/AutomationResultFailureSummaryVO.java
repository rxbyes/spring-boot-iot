package com.ghlzm.iot.report.vo;

import lombok.Data;

import java.util.Map;

/**
 * 自动化运行失败分布摘要。
 */
@Data
public class AutomationResultFailureSummaryVO {

    private String primaryCategory;
    private Map<String, Integer> countsByCategory;
}
