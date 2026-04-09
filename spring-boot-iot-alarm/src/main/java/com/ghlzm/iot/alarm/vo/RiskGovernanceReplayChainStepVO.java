package com.ghlzm.iot.alarm.vo;

import lombok.Data;

/**
 * 自动化复盘串联步骤。
 */
@Data
public class RiskGovernanceReplayChainStepVO {

    private String stepCode;

    private String stepLabel;

    private String status;

    private String summary;

    private String nextAction;
}
