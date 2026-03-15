package com.ghlzm.iot.message.http;

import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.message.service.DownMessageService;
import com.ghlzm.iot.message.service.model.DownMessagePublishCommand;
import com.ghlzm.iot.message.service.model.DownMessagePublishResult;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * MQTT 下行发布控制器。
 * 控制层只负责接收请求并转给 service 层。
 */
@RestController
public class DeviceDownController {

    private final DownMessageService downMessageService;

    public DeviceDownController(DownMessageService downMessageService) {
        this.downMessageService = downMessageService;
    }

    @PostMapping("/message/mqtt/down/publish")
    public R<DownMessagePublishResult> publish(@RequestBody @Valid DeviceDownPublishRequest request) {
        DownMessagePublishCommand command = new DownMessagePublishCommand();
        command.setProtocolCode(request.getProtocolCode());
        command.setProductKey(request.getProductKey());
        command.setDeviceCode(request.getDeviceCode());
        command.setTopic(request.getTopic());
        command.setQos(request.getQos());
        command.setRetained(request.getRetained());
        command.setMessageId(request.getMessageId());
        command.setCommandType(request.getCommandType());
        command.setServiceIdentifier(request.getServiceIdentifier());
        command.setParams(request.getParams());
        return R.ok(downMessageService.publish(command));
    }
}
