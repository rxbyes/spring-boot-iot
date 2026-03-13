package com.ghlzm.iot.protocol.core.adapter;

import com.ghlzm.iot.protocol.core.context.ProtocolContext;
import com.ghlzm.iot.protocol.core.model.DeviceDownMessage;
import com.ghlzm.iot.protocol.core.model.DeviceUpMessage;

/**
 * 协议适配器
 * Author rxbyes
 * Since 2.0
 * Date 2026/3/13 - 14:05
 */
public interface ProtocolAdapter {

    String getProtocolCode();

    DeviceUpMessage decode(byte[] payload, ProtocolContext context);

    byte[] encode(DeviceDownMessage message, ProtocolContext context);
}

