package com.ghlzm.iot.message.http;

import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.message.http.vo.MessageFlowSubmitResultVO;
import com.ghlzm.iot.message.service.MqttReportPublishService;
import com.ghlzm.iot.framework.observability.messageflow.MessageFlowSubmitResult;
import com.ghlzm.iot.message.service.model.MqttReportPublishCommand;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * MQTT 原始上行模拟发布控制器。
 */
@RestController
public class DeviceMqttReportController {

    private final MqttReportPublishService mqttReportPublishService;

    public DeviceMqttReportController(MqttReportPublishService mqttReportPublishService) {
        this.mqttReportPublishService = mqttReportPublishService;
    }

    @PostMapping("/api/message/mqtt/report/publish")
    public R<MessageFlowSubmitResultVO> publish(@RequestBody @Valid DeviceMqttReportPublishRequest request) {
        MqttReportPublishCommand command = new MqttReportPublishCommand();
        command.setProtocolCode(request.getProtocolCode());
        command.setProductKey(request.getProductKey());
        command.setDeviceCode(request.getDeviceCode());
        command.setTopic(request.getTopic());
        command.setPayload(request.getPayload());
        command.setPayloadEncoding(request.getPayloadEncoding());
        command.setQos(request.getQos());
        command.setRetained(request.getRetained());
        MessageFlowSubmitResult submitResult = mqttReportPublishService.publish(command);
        MessageFlowSubmitResultVO resultVO = new MessageFlowSubmitResultVO();
        resultVO.setSessionId(submitResult.getSessionId());
        resultVO.setTraceId(submitResult.getTraceId());
        resultVO.setStatus(submitResult.getStatus());
        resultVO.setTimelineAvailable(submitResult.getTimelineAvailable());
        resultVO.setCorrelationPending(submitResult.getCorrelationPending());
        return R.ok(resultVO);
    }
}
