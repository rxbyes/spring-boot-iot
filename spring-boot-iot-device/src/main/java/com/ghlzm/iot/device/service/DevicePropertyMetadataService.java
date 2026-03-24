package com.ghlzm.iot.device.service;

import com.ghlzm.iot.device.service.model.DevicePropertyMetadata;

import java.util.Map;

/**
 * 产品物模型属性元数据解析服务。
 */
public interface DevicePropertyMetadataService {

    /**
     * 查询产品下的属性元数据。
     */
    Map<String, DevicePropertyMetadata> listPropertyMetadataMap(Long productId);
}
