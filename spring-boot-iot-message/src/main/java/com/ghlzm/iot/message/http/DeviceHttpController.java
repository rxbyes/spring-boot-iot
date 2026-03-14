package com.ghlzm.iot.message.http;

import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.message.dispatcher.UpMessageDispatcher;
import com.ghlzm.iot.protocol.core.model.RawDeviceMessage;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

/**
 * HTTP 设备接入控制器。
 * 控制器只负责把 HTTP 请求转换成统一原始消息，再交给分发层。
 *
 * Author rxbyes
 * Since 2.0
 * Date 2026/3/13 - 14:36
 */
@RestController
public class DeviceHttpController {

    private final UpMessageDispatcher upMessageDispatcher;

    public DeviceHttpController(UpMessageDispatcher upMessageDispatcher) {
        this.upMessageDispatcher = upMessageDispatcher;
    }

    @PostMapping("/message/http/report")
    public R<?> report(@RequestBody @Valid DeviceReportRequest request) {
        // 入口层不做协议和业务判断，只构造统一原始消息对象。
        RawDeviceMessage raw = new RawDeviceMessage();
        raw.setProtocolCode(request.getProtocolCode());
        raw.setProductKey(request.getProductKey());
        raw.setDeviceCode(request.getDeviceCode());
        raw.setTopic(request.getTopic());
        raw.setClientId(request.getClientId());
        raw.setTenantId(request.getTenantId());
        raw.setPayload(request.getPayload().getBytes(StandardCharsets.UTF_8));

        upMessageDispatcher.dispatch(raw);
        return R.ok();
    }
}
