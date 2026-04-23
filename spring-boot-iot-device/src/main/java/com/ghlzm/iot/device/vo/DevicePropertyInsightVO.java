package com.ghlzm.iot.device.vo;

import com.ghlzm.iot.device.entity.DeviceProperty;
import com.ghlzm.iot.device.model.DeviceTopologyRole;
import java.util.List;
import lombok.Data;

@Data
public class DevicePropertyInsightVO {
    private DeviceTopologyRole topologyRole;
    private List<DeviceProperty> properties;
}
