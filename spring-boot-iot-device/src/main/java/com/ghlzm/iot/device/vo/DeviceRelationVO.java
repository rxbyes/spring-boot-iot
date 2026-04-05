package com.ghlzm.iot.device.vo;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 设备关系返回对象。
 */
@Data
public class DeviceRelationVO {

    private Long id;
    private String parentDeviceCode;
    private String logicalChannelCode;
    private String childDeviceCode;
    private Long childProductId;
    private String childProductKey;
    private String relationType;
    private String canonicalizationStrategy;
    private String statusMirrorStrategy;
    private Integer enabled;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
