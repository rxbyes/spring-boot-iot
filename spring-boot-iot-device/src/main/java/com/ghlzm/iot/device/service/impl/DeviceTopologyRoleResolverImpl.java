package com.ghlzm.iot.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.DeviceRelation;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.mapper.DeviceMapper;
import com.ghlzm.iot.device.mapper.DeviceRelationMapper;
import com.ghlzm.iot.device.mapper.ProductMapper;
import com.ghlzm.iot.device.model.DeviceTopologyRole;
import com.ghlzm.iot.device.service.DeviceTopologyRoleResolver;
import org.springframework.stereotype.Service;

@Service
public class DeviceTopologyRoleResolverImpl implements DeviceTopologyRoleResolver {

    private static final String COLLECTOR_RTU_PRODUCT_KEY = "nf-collect-rtu-v1";
    private static final int NODE_TYPE_COLLECTOR = 2;

    private final DeviceMapper deviceMapper;
    private final DeviceRelationMapper deviceRelationMapper;
    private final ProductMapper productMapper;

    public DeviceTopologyRoleResolverImpl(DeviceMapper deviceMapper,
                                          DeviceRelationMapper deviceRelationMapper,
                                          ProductMapper productMapper) {
        this.deviceMapper = deviceMapper;
        this.deviceRelationMapper = deviceRelationMapper;
        this.productMapper = productMapper;
    }

    @Override
    public DeviceTopologyRole resolve(Long productId, Integer nodeType, String productKey) {
        if (nodeType != null && nodeType == NODE_TYPE_COLLECTOR) {
            return DeviceTopologyRole.COLLECTOR_PARENT;
        }
        if (isCollectorRtuProduct(productKey)) {
            return DeviceTopologyRole.COLLECTOR_PARENT;
        }
        if (isCollectorChildByRelation(productId)) {
            return DeviceTopologyRole.COLLECTOR_CHILD;
        }
        return DeviceTopologyRole.STANDALONE;
    }

    @Override
    public DeviceTopologyRole resolveByDeviceCode(String deviceCode) {
        Device device = deviceMapper.selectOne(
            new LambdaQueryWrapper<Device>()
                .eq(Device::getDeviceCode, deviceCode)
        );
        if (device == null) {
            throw new IllegalArgumentException("Device not found: " + deviceCode);
        }
        String productKey = null;
        if (device.getProductId() != null) {
            Product product = productMapper.selectById(device.getProductId());
            if (product != null) {
                productKey = product.getProductKey();
            }
        }
        return resolve(device.getProductId(), device.getNodeType(), productKey);
    }

    private boolean isCollectorRtuProduct(String productKey) {
        return productKey != null
            && productKey.trim().equalsIgnoreCase(COLLECTOR_RTU_PRODUCT_KEY);
    }

    private boolean isCollectorChildByRelation(Long productId) {
        if (productId == null) {
            return false;
        }
        return deviceRelationMapper.exists(
            new LambdaQueryWrapper<DeviceRelation>()
                .eq(DeviceRelation::getChildProductId, productId)
        );
    }
}
