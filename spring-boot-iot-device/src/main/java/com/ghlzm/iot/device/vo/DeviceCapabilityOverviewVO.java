package com.ghlzm.iot.device.vo;

import lombok.Data;

import java.util.List;

@Data
public class DeviceCapabilityOverviewVO {

    private String deviceCode;

    private Long productId;

    private String productKey;

    private String productCapabilityType;

    private String subType;

    private boolean onlineExecutable;

    private String disabledReason;

    private List<DeviceCapabilityVO> capabilities;
}
