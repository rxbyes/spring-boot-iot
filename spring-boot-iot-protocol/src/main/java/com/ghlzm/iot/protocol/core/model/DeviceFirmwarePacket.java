package com.ghlzm.iot.protocol.core.model;

import lombok.Data;

/**
 * 统一固件分包协议模型。
 * 当前仅在协议层生成，后续 OTA 模块可直接消费该对象。
 */
@Data
public class DeviceFirmwarePacket {

    private Integer packetIndex;
    private Integer packetSize;
    private Integer totalPackets;
    private byte[] packetData;
    private Integer md5Length;
    private String firmwareMd5;
}
