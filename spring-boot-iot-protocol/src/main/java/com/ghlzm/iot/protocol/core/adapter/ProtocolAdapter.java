package com.ghlzm.iot.protocol.core.adapter;

import com.ghlzm.iot.protocol.core.context.ProtocolContext;
import com.ghlzm.iot.protocol.core.model.DeviceDownMessage;
import com.ghlzm.iot.protocol.core.model.DeviceUpMessage;

/**
 * 协议适配器抽象。
 * protocol 模块只负责协议报文与统一消息模型之间的转换。
 *
 * Author rxbyes
 * Since 2.0
 * Date 2026/3/13 - 14:05
 */
public interface ProtocolAdapter {

    /**
     * 获取当前适配器支持的协议编码。
     */
    String getProtocolCode();

    /**
     * 把原始报文解码成统一上行消息。
     */
    DeviceUpMessage decode(byte[] payload, ProtocolContext context);

    /**
     * 把统一下行消息编码成协议报文。
     */
    byte[] encode(DeviceDownMessage message, ProtocolContext context);
}
