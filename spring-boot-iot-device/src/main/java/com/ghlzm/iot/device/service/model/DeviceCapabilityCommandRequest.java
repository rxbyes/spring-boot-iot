package com.ghlzm.iot.device.service.model;

import com.ghlzm.iot.device.capability.DeviceCapabilityDefinition;
import com.ghlzm.iot.device.capability.ProductCapabilityMetadata;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.Product;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class DeviceCapabilityCommandRequest {

    private Long currentUserId;

    private Device device;

    private Product product;

    private ProductCapabilityMetadata metadata;

    private DeviceCapabilityDefinition capability;

    private Map<String, Object> params = new LinkedHashMap<>();

    private String commandId;
}
