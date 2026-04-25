package com.ghlzm.iot.report.vo;

import lombok.Data;

import java.util.List;

/**
 * 自动化结果归档筛选维度。
 */
@Data
public class AutomationResultArchiveFacetVO {

    private List<String> statuses;
    private List<String> runnerTypes;
    private List<String> packageCodes;
    private List<String> environmentCodes;
}
