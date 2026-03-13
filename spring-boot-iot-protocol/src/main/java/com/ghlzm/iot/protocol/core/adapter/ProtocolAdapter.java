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

    /**
     * 获取协议编码
     *
     * @return
     */
    String getProtocolCode();

    /**
     * 解码
     *
     * @param payload
     * @param context
     * @return
     */
    DeviceUpMessage decode(byte[] payload, ProtocolContext context);

    /**
     * 编码
     *
     * @param message
     * @param context
     * @return
     */
    byte[] encode(DeviceDownMessage message, ProtocolContext context);
}

