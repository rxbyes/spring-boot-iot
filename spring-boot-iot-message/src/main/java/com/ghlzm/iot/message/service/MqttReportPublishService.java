package com.ghlzm.iot.message.service;

import com.ghlzm.iot.framework.observability.messageflow.MessageFlowSubmitResult;
import com.ghlzm.iot.message.service.model.MqttReportPublishCommand;

/**
 * MQTT 原始上行模拟发布服务。
 */
public interface MqttReportPublishService {

    /**
     * 直接向 Broker 发布原始上行报文，随后由现有 consumer 回流主链路。
     */
    MessageFlowSubmitResult publish(MqttReportPublishCommand command);
}
