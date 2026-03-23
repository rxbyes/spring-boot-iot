package com.ghlzm.iot.message.pipeline;

import com.ghlzm.iot.framework.observability.messageflow.MessageFlowSubmitResult;
import com.ghlzm.iot.framework.observability.messageflow.MessageFlowTimeline;
import com.ghlzm.iot.protocol.core.model.DeviceUpMessage;
import com.ghlzm.iot.protocol.core.model.RawDeviceMessage;
import lombok.Data;

/**
 * pipeline 执行结果。
 */
@Data
public class MessageFlowExecutionResult {

    private MessageFlowSubmitResult submitResult;
    private MessageFlowTimeline timeline;
    private RawDeviceMessage rawDeviceMessage;
    private DeviceUpMessage upMessage;
}
