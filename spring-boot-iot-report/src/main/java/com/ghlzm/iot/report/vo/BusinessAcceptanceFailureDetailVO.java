package com.ghlzm.iot.report.vo;

import lombok.Data;

/**
 * 业务验收失败明细
 */
@Data
public class BusinessAcceptanceFailureDetailVO {

    private String scenarioId;
    private String scenarioTitle;
    private String stepLabel;
    private String apiRef;
    private String pageAction;
    private String summary;
}
