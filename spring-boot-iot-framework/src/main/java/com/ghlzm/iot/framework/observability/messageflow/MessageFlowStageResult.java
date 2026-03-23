package com.ghlzm.iot.framework.observability.messageflow;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 单个 stage 执行结果。
 */
@Data
public class MessageFlowStageResult {

    private String status = MessageFlowStatuses.STEP_SUCCESS;
    private String branch;
    private String errorClass;
    private String errorMessage;
    private Map<String, Object> summary = new LinkedHashMap<>();
    private List<MessageFlowStep> additionalSteps = new ArrayList<>();
}
