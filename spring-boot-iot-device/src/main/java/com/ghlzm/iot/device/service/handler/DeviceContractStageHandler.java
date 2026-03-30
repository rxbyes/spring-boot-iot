package com.ghlzm.iot.device.service.handler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ghlzm.iot.common.enums.ProductStatusEnum;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.mapper.DeviceMapper;
import com.ghlzm.iot.device.mapper.ProductMapper;
import com.ghlzm.iot.device.service.model.DeviceProcessingTarget;
import com.ghlzm.iot.protocol.core.model.DeviceUpMessage;
import org.springframework.stereotype.Component;

@Component
public class DeviceContractStageHandler {

    private final DeviceMapper deviceMapper;
    private final ProductMapper productMapper;

    public DeviceContractStageHandler(DeviceMapper deviceMapper, ProductMapper productMapper) {
        this.deviceMapper = deviceMapper;
        this.productMapper = productMapper;
    }

    public DeviceProcessingTarget resolve(DeviceUpMessage upMessage) {
        Device device = findDevice(upMessage);
        if (device == null) {
            throw new BizException("设备不存在: " + upMessage.getDeviceCode());
        }
        Product product = getRequiredProduct(device);
        ensureProductEnabledForAccess(product);
        validateProductMatched(upMessage, device, product);
        validateProtocolMatched(upMessage, device, product);

        DeviceProcessingTarget target = new DeviceProcessingTarget();
        target.setDevice(device);
        target.setProduct(product);
        target.setMessage(upMessage);
        return target;
    }

    private Device findDevice(DeviceUpMessage upMessage) {
        String deviceCode = normalizeText(upMessage == null ? null : upMessage.getDeviceCode());
        if (!hasText(deviceCode)) {
            return null;
        }
        LambdaQueryWrapper<Device> queryWrapper = new LambdaQueryWrapper<Device>()
                .eq(Device::getDeviceCode, deviceCode)
                .eq(Device::getDeleted, 0);
        Long tenantId = parseTenantId(upMessage == null ? null : upMessage.getTenantId());
        if (tenantId != null) {
            queryWrapper.eq(Device::getTenantId, tenantId);
        }
        return deviceMapper.selectOne(queryWrapper.last("limit 1"));
    }

    private Product getRequiredProduct(Device device) {
        if (device.getProductId() == null) {
            throw new BizException("设备未绑定产品: " + device.getDeviceCode());
        }
        Product product = productMapper.selectById(device.getProductId());
        if (product == null || Integer.valueOf(1).equals(product.getDeleted())) {
            throw new BizException("设备所属产品不存在: " + device.getDeviceCode());
        }
        return product;
    }

    private void ensureProductEnabledForAccess(Product product) {
        if (product != null && ProductStatusEnum.DISABLED.getCode().equals(product.getStatus())) {
            throw new BizException("产品已停用，拒绝设备接入: " + product.getProductKey());
        }
    }

    private void validateProductMatched(DeviceUpMessage upMessage, Device device, Product product) {
        String expectedProductKey = normalizeText(product == null ? null : product.getProductKey());
        String actualProductKey = hasText(upMessage.getProductKey())
                ? upMessage.getProductKey().trim()
                : expectedProductKey;
        if (!hasText(expectedProductKey) || !hasText(actualProductKey)
                || !expectedProductKey.equalsIgnoreCase(actualProductKey)) {
            throw new BizException("设备所属产品不匹配: " + device.getDeviceCode()
                    + ", expected=" + displayText(expectedProductKey)
                    + ", actual=" + displayText(actualProductKey));
        }
        upMessage.setProductKey(expectedProductKey);
    }

    private void validateProtocolMatched(DeviceUpMessage upMessage, Device device, Product product) {
        String deviceProtocolCode = normalizeText(device == null ? null : device.getProtocolCode());
        String productProtocolCode = normalizeText(product == null ? null : product.getProtocolCode());
        if (hasText(deviceProtocolCode) && hasText(productProtocolCode)
                && !deviceProtocolCode.equalsIgnoreCase(productProtocolCode)) {
            throw new BizException("设备协议配置异常: " + device.getDeviceCode()
                    + ", deviceProtocol=" + deviceProtocolCode
                    + ", productProtocol=" + productProtocolCode);
        }

        String expectedProtocolCode = hasText(deviceProtocolCode) ? deviceProtocolCode : productProtocolCode;
        String actualProtocolCode = hasText(upMessage.getProtocolCode())
                ? upMessage.getProtocolCode().trim()
                : expectedProtocolCode;
        if (!hasText(expectedProtocolCode)) {
            throw new BizException("设备接入协议未配置: " + device.getDeviceCode()
                    + ", deviceProtocol=" + displayText(deviceProtocolCode)
                    + ", productProtocol=" + displayText(productProtocolCode));
        }
        if (!hasText(actualProtocolCode) || !expectedProtocolCode.equalsIgnoreCase(actualProtocolCode)) {
            throw new BizException("设备协议不匹配: " + device.getDeviceCode()
                    + ", expected=" + expectedProtocolCode
                    + ", actual=" + displayText(actualProtocolCode));
        }
        upMessage.setProtocolCode(expectedProtocolCode);
    }

    private Long parseTenantId(String tenantId) {
        String normalizedTenantId = normalizeText(tenantId);
        if (!hasText(normalizedTenantId)) {
            return null;
        }
        try {
            return Long.parseLong(normalizedTenantId);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String normalizeText(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private String displayText(String value) {
        return hasText(value) ? value.trim() : "<empty>";
    }
}
