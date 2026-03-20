package com.ghlzm.iot.message.service;

import com.ghlzm.iot.message.service.model.DownMessagePublishCommand;
import com.ghlzm.iot.message.service.model.DownMessagePublishResult;

/**
 * MQTT 下行消息服务。
 * service 层负责下行 topic 解析、协议上下文组装和发布编排。
 */
public interface DownMessageService {

    /**
     * 发布最小下行消息。
     */
    DownMessagePublishResult publish(DownMessagePublishCommand command);
}
