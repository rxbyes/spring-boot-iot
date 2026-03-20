package com.ghlzm.iot.common.enums;


import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Author rxbyes
 * Since 2.0
 * Date 2026/3/13 - 13:17
 */
@Getter
@AllArgsConstructor
public enum ProtocolTypeEnum {

    MQTT_JSON("mqtt-json", "MQTT-JSON"),
    TCP_HEX("tcp-hex", "TCP-HEX"),
    MODBUS_TCP("modbus-tcp", "Modbus TCP"),
    MODBUS_RTU("modbus-rtu", "Modbus RTU");

    private final String code;
    private final String desc;
}
