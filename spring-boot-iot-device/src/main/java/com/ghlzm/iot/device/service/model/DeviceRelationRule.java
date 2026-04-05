package com.ghlzm.iot.device.service.model;

import lombok.Data;

/**
 * 设备关系运行时规则。
 */
@Data
public class DeviceRelationRule {

    private Long relationId;
    private String parentDeviceCode;
    private String logicalChannelCode;
    private String childDeviceCode;
    private Long childProductId;
    private String childProductKey;
    private String relationType;
    private String canonicalizationStrategy;
    private String statusMirrorStrategy;
}
