package com.ghlzm.iot.protocol.mqtt.legacy;

/**
 * legacy `$dp` 逻辑通道到子设备的运行时规则。
 */
public record LegacyDpRelationRule(String logicalChannelCode,
                                   String childDeviceCode,
                                   String canonicalizationStrategy,
                                   String statusMirrorStrategy) {
}
