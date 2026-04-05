package com.ghlzm.iot.device.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ghlzm.iot.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 设备逻辑通道关系。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("iot_device_relation")
public class DeviceRelation extends BaseEntity {

    private Long parentDeviceId;
    private String parentDeviceCode;
    private String logicalChannelCode;
    private Long childDeviceId;
    private String childDeviceCode;
    private Long childProductId;
    private String childProductKey;
    private String relationType;
    private String canonicalizationStrategy;
    private String statusMirrorStrategy;
    private Integer enabled;
}
