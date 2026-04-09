package com.ghlzm.iot.alarm.vo;

import com.ghlzm.iot.device.entity.DeviceAccessErrorLog;
import com.ghlzm.iot.device.entity.DeviceMessageLog;
import com.ghlzm.iot.device.vo.messageflow.MessageTraceDetailVO;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 风险治理回放结果。
 */
@Data
public class RiskGovernanceReplayVO {

    private String traceId;

    private String deviceCode;

    private String productKey;

    private Long releaseBatchId;

    private String releaseScenarioCode;

    private Long matchedMessageCount;

    private Long matchedAccessErrorCount;

    private List<DeviceMessageLog> recentMessages = new ArrayList<>();

    private List<DeviceAccessErrorLog> recentAccessErrors = new ArrayList<>();

    private MessageTraceDetailVO latestMessageDetail;

    private RiskGovernanceReplayGapSummaryVO gapSummary;

    private RiskGovernanceReplayBatchReconciliationVO batchReconciliation;

    private List<RiskGovernanceReplayChainStepVO> replayChainSteps = new ArrayList<>();
}
