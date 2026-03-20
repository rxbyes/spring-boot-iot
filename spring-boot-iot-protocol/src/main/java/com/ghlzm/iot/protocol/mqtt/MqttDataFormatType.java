package com.ghlzm.iot.protocol.mqtt;

import com.ghlzm.iot.common.exception.BizException;
import lombok.Getter;

/**
 * MQTT 历史设备数据格式类型。
 * 当前已根据附件表 C.1 / C.2 / C.3 明确三类格式。
 */
@Getter
public enum MqttDataFormatType {

    DIRECT_JSON_COMPAT((byte) 0, "历史兼容直接 JSON", true),
    STANDARD_TYPE_1((byte) 1, "表 C.1 数据点格式类型一", true),
    STANDARD_TYPE_2((byte) 2, "表 C.2 数据点格式类型二", true),
    STANDARD_TYPE_3((byte) 3, "表 C.3 数据点格式类型三", true);

    private final byte code;
    private final String description;
    private final boolean supported;

    MqttDataFormatType(byte code, String description, boolean supported) {
        this.code = code;
        this.description = description;
        this.supported = supported;
    }

    public static MqttDataFormatType fromByte(byte code) {
        for (MqttDataFormatType value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        throw new BizException("未知数据格式类型: " + (code & 0xFF));
    }
}
