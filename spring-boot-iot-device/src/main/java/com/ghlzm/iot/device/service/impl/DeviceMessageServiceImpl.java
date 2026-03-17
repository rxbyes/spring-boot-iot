package com.ghlzm.iot.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.device.dto.DeviceMessageTraceQuery;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.DeviceMessageLog;
import com.ghlzm.iot.device.entity.DeviceProperty;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.entity.ProductModel;
import com.ghlzm.iot.device.mapper.DeviceMapper;
import com.ghlzm.iot.device.mapper.DeviceMessageLogMapper;
import com.ghlzm.iot.device.mapper.DevicePropertyMapper;
import com.ghlzm.iot.device.mapper.ProductMapper;
import com.ghlzm.iot.device.mapper.ProductModelMapper;
import com.ghlzm.iot.device.service.CommandRecordService;
import com.ghlzm.iot.device.service.DeviceFileService;
import com.ghlzm.iot.device.service.DeviceMessageService;
import com.ghlzm.iot.framework.config.IotProperties;
import com.ghlzm.iot.framework.observability.TraceContextHolder;
import com.ghlzm.iot.protocol.core.model.DeviceUpMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 设备上行消息处理服务。
 */
@Service
public class DeviceMessageServiceImpl implements DeviceMessageService {

    private static final Logger log = LoggerFactory.getLogger(DeviceMessageServiceImpl.class);
    private static final List<String> COMMAND_ID_ALIASES = List.of("commandId", "messageId");
    private static final List<String> ERROR_MESSAGE_ALIASES = List.of("errorMessage", "error", "msg", "message");

    private final DeviceMapper deviceMapper;
    private final DeviceMessageLogMapper deviceMessageLogMapper;
    private final DevicePropertyMapper devicePropertyMapper;
    private final ProductMapper productMapper;
    private final ProductModelMapper productModelMapper;
    private final CommandRecordService commandRecordService;
    private final DeviceFileService deviceFileService;
    private final IotProperties iotProperties;
    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

    public DeviceMessageServiceImpl(DeviceMapper deviceMapper,
                                    DeviceMessageLogMapper deviceMessageLogMapper,
                                    DevicePropertyMapper devicePropertyMapper,
                                    ProductMapper productMapper,
                                    ProductModelMapper productModelMapper,
                                    CommandRecordService commandRecordService,
                                    DeviceFileService deviceFileService,
                                    IotProperties iotProperties) {
        this.deviceMapper = deviceMapper;
        this.deviceMessageLogMapper = deviceMessageLogMapper;
        this.devicePropertyMapper = devicePropertyMapper;
        this.productMapper = productMapper;
        this.productModelMapper = productModelMapper;
        this.commandRecordService = commandRecordService;
        this.deviceFileService = deviceFileService;
        this.iotProperties = iotProperties;
    }

    @Override
    public List<DeviceMessageLog> listMessageLogs(String deviceCode) {
        Device device = findDeviceByCode(deviceCode);
        if (device == null) {
            throw new BizException("设备不存在: " + deviceCode);
        }
        return deviceMessageLogMapper.selectList(
                new LambdaQueryWrapper<DeviceMessageLog>()
                        .eq(DeviceMessageLog::getDeviceId, device.getId())
                        .orderByDesc(DeviceMessageLog::getReportTime)
                        .last("limit 20")
        );
    }

    @Override
    public PageResult<DeviceMessageLog> pageMessageTraceLogs(DeviceMessageTraceQuery query, Integer pageNum, Integer pageSize) {
        long safePageNum = pageNum == null || pageNum < 1 ? 1L : pageNum;
        long safePageSize = pageSize == null || pageSize < 1 ? 10L : Math.min(pageSize, 100);
        Page<DeviceMessageLog> page = new Page<>(safePageNum, safePageSize);
        Page<DeviceMessageLog> result = deviceMessageLogMapper.selectPage(page, buildMessageTraceQueryWrapper(query));
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleUpMessage(DeviceUpMessage upMessage) {
        Device device = findDeviceByCode(upMessage.getDeviceCode());
        if (device == null) {
            throw new BizException("设备不存在: " + upMessage.getDeviceCode());
        }
        if (device.getProtocolCode() == null || !device.getProtocolCode().equals(upMessage.getProtocolCode())) {
            throw new BizException("设备协议不匹配: " + upMessage.getDeviceCode());
        }

        String productKey = fetchProductKey(device);
        if (!hasText(upMessage.getProductKey())) {
            upMessage.setProductKey(productKey);
        }
        if (!hasText(productKey) || !hasText(upMessage.getProductKey())
                || !upMessage.getProductKey().equalsIgnoreCase(productKey)) {
            throw new BizException("设备所属产品不匹配: " + upMessage.getDeviceCode());
        }

        saveMessageLog(device, upMessage);
        if (isCommandReply(upMessage)) {
            handleCommandReply(device, upMessage);
            updateDeviceOnlineStatus(device, upMessage);
            return;
        }

        // 文件/固件场景先交给文件服务处理，避免混入普通属性更新。
        deviceFileService.handleFilePayload(device, upMessage);
        updateLatestProperties(device, upMessage);
        updateDeviceOnlineStatus(device, upMessage);
    }

    private Device findDeviceByCode(String deviceCode) {
        return deviceMapper.selectOne(
                new LambdaQueryWrapper<Device>()
                        .eq(Device::getDeviceCode, deviceCode)
                        .eq(Device::getDeleted, 0)
                        .last("limit 1")
        );
    }

    private LambdaQueryWrapper<DeviceMessageLog> buildMessageTraceQueryWrapper(DeviceMessageTraceQuery query) {
        LambdaQueryWrapper<DeviceMessageLog> queryWrapper = new LambdaQueryWrapper<>();
        if (query != null) {
            if (StringUtils.hasText(query.getDeviceCode())) {
                queryWrapper.eq(DeviceMessageLog::getDeviceCode, query.getDeviceCode().trim());
            }
            if (StringUtils.hasText(query.getProductKey())) {
                queryWrapper.eq(DeviceMessageLog::getProductKey, query.getProductKey().trim());
            }
            if (StringUtils.hasText(query.getTraceId())) {
                queryWrapper.eq(DeviceMessageLog::getTraceId, query.getTraceId().trim());
            }
            if (StringUtils.hasText(query.getMessageType())) {
                queryWrapper.eq(DeviceMessageLog::getMessageType, query.getMessageType().trim());
            }
            if (StringUtils.hasText(query.getTopic())) {
                queryWrapper.like(DeviceMessageLog::getTopic, query.getTopic().trim());
            }
        }
        queryWrapper.orderByDesc(DeviceMessageLog::getReportTime)
                .orderByDesc(DeviceMessageLog::getCreateTime);
        return queryWrapper;
    }

    private void saveMessageLog(Device device, DeviceUpMessage upMessage) {
        DeviceMessageLog logRecord = new DeviceMessageLog();
        logRecord.setTenantId(device.getTenantId());
        logRecord.setDeviceId(device.getId());
        logRecord.setProductId(device.getProductId());
        logRecord.setTraceId(hasText(upMessage.getTraceId()) ? upMessage.getTraceId() : TraceContextHolder.getTraceId());
        logRecord.setDeviceCode(device.getDeviceCode());
        logRecord.setProductKey(upMessage.getProductKey());
        logRecord.setMessageType(upMessage.getMessageType());
        logRecord.setTopic(upMessage.getTopic());
        logRecord.setPayload(upMessage.getRawPayload());
        logRecord.setReportTime(upMessage.getTimestamp() == null ? LocalDateTime.now() : upMessage.getTimestamp());
        logRecord.setCreateTime(LocalDateTime.now());
        deviceMessageLogMapper.insert(logRecord);
    }

    private boolean isCommandReply(DeviceUpMessage upMessage) {
        return upMessage != null && "reply".equalsIgnoreCase(upMessage.getMessageType());
    }

    private void handleCommandReply(Device device, DeviceUpMessage upMessage) {
        Map<String, Object> replyPayload = parseReplyPayload(upMessage.getRawPayload());
        if (replyPayload.isEmpty()) {
            log.warn("设备 ACK 回执无法解析为 JSON, deviceCode={}, topic={}", device.getDeviceCode(), upMessage.getTopic());
            return;
        }

        String commandId = resolveCommandId(replyPayload);
        if (!hasText(commandId)) {
            log.warn("设备 ACK 回执缺少 commandId/messageId, deviceCode={}, topic={}", device.getDeviceCode(), upMessage.getTopic());
            return;
        }

        LocalDateTime ackTime = upMessage.getTimestamp() == null ? LocalDateTime.now() : upMessage.getTimestamp();
        boolean updated;
        if (isReplySuccess(replyPayload)) {
            updated = commandRecordService.markSuccessByCommandId(commandId, upMessage.getRawPayload(), ackTime);
        } else {
            updated = commandRecordService.markFailedByCommandId(
                    commandId,
                    upMessage.getRawPayload(),
                    resolveReplyErrorMessage(replyPayload),
                    ackTime
            );
        }

        if (!updated) {
            log.warn("设备 ACK 回执未找到匹配命令记录, deviceCode={}, commandId={}, topic={}",
                    device.getDeviceCode(), commandId, upMessage.getTopic());
        }
    }

    private void updateLatestProperties(Device device, DeviceUpMessage upMessage) {
        Map<String, Object> properties = upMessage.getProperties();
        if (properties == null || properties.isEmpty()) {
            return;
        }

        Map<String, ProductModel> propertyModels = listPropertyModels(device.getProductId());
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            String identifier = entry.getKey();
            Object value = entry.getValue();
            ProductModel productModel = propertyModels.get(identifier);

            DeviceProperty property = devicePropertyMapper.selectOne(
                    new LambdaQueryWrapper<DeviceProperty>()
                            .eq(DeviceProperty::getDeviceId, device.getId())
                            .eq(DeviceProperty::getIdentifier, identifier)
                            .last("limit 1")
            );

            if (property == null) {
                property = new DeviceProperty();
                property.setTenantId(device.getTenantId());
                property.setDeviceId(device.getId());
                property.setIdentifier(identifier);
                property.setPropertyName(productModel == null ? identifier : productModel.getModelName());
                property.setPropertyValue(value == null ? null : String.valueOf(value));
                property.setValueType(resolveValueType(value, productModel));
                property.setReportTime(upMessage.getTimestamp() == null ? LocalDateTime.now() : upMessage.getTimestamp());
                property.setCreateTime(LocalDateTime.now());
                property.setUpdateTime(LocalDateTime.now());
                devicePropertyMapper.insert(property);
            } else {
                property.setPropertyName(productModel == null ? property.getPropertyName() : productModel.getModelName());
                property.setPropertyValue(value == null ? null : String.valueOf(value));
                property.setValueType(resolveValueType(value, productModel));
                property.setReportTime(upMessage.getTimestamp() == null ? LocalDateTime.now() : upMessage.getTimestamp());
                property.setUpdateTime(LocalDateTime.now());
                devicePropertyMapper.updateById(property);
            }
        }
    }

    private void updateDeviceOnlineStatus(Device device, DeviceUpMessage upMessage) {
        LocalDateTime reportTime = upMessage.getTimestamp() == null ? LocalDateTime.now() : upMessage.getTimestamp();

        Device update = new Device();
        update.setId(device.getId());
        update.setOnlineStatus(1);
        update.setLastOnlineTime(reportTime);
        update.setLastReportTime(reportTime);
        boolean activateDefault = iotProperties.getDevice() != null
                && Boolean.TRUE.equals(iotProperties.getDevice().getActivateDefault());
        if (activateDefault) {
            update.setActivateStatus(1);
        }
        deviceMapper.updateById(update);
    }

    private Map<String, ProductModel> listPropertyModels(Long productId) {
        List<ProductModel> productModels = productModelMapper.selectList(
                new LambdaQueryWrapper<ProductModel>()
                        .eq(ProductModel::getProductId, productId)
                        .eq(ProductModel::getModelType, "property")
                        .eq(ProductModel::getDeleted, 0)
        );
        return productModels.stream()
                .collect(Collectors.toMap(ProductModel::getIdentifier, Function.identity(), (left, right) -> left));
    }

    private String resolveValueType(Object value, ProductModel productModel) {
        if (productModel != null && hasText(productModel.getDataType())) {
            return productModel.getDataType();
        }
        if (value == null) {
            return "string";
        }
        if (value instanceof Integer || value instanceof Long) {
            return "int";
        }
        if (value instanceof Float || value instanceof Double) {
            return "double";
        }
        if (value instanceof Boolean) {
            return "bool";
        }
        return "string";
    }

    private String fetchProductKey(Device device) {
        Product product = productMapper.selectById(device.getProductId());
        return product == null ? null : product.getProductKey();
    }

    private Map<String, Object> parseReplyPayload(String rawPayload) {
        if (!hasText(rawPayload)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(rawPayload, new TypeReference<>() {
            });
        } catch (Exception ex) {
            return Map.of();
        }
    }

    private String resolveCommandId(Map<String, Object> replyPayload) {
        for (String alias : COMMAND_ID_ALIASES) {
            Object value = replyPayload.get(alias);
            if (value != null && hasText(String.valueOf(value))) {
                return String.valueOf(value);
            }
        }
        return null;
    }

    private boolean isReplySuccess(Map<String, Object> replyPayload) {
        Object success = replyPayload.get("success");
        if (success != null) {
            return parseBooleanLike(success);
        }

        Object code = replyPayload.get("code");
        if (code != null) {
            String normalized = String.valueOf(code).trim();
            return "0".equals(normalized)
                    || "200".equals(normalized)
                    || "ok".equalsIgnoreCase(normalized)
                    || "success".equalsIgnoreCase(normalized);
        }

        Object status = replyPayload.get("status");
        if (status != null) {
            String normalized = String.valueOf(status).trim();
            if ("failed".equalsIgnoreCase(normalized) || "error".equalsIgnoreCase(normalized)) {
                return false;
            }
            if ("success".equalsIgnoreCase(normalized) || "ok".equalsIgnoreCase(normalized)) {
                return true;
            }
        }

        return true;
    }

    private boolean parseBooleanLike(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        String normalized = String.valueOf(value).trim();
        return "1".equals(normalized)
                || "true".equalsIgnoreCase(normalized)
                || "yes".equalsIgnoreCase(normalized)
                || "ok".equalsIgnoreCase(normalized)
                || "success".equalsIgnoreCase(normalized);
    }

    private String resolveReplyErrorMessage(Map<String, Object> replyPayload) {
        for (String alias : ERROR_MESSAGE_ALIASES) {
            Object value = replyPayload.get(alias);
            if (value != null && hasText(String.valueOf(value))) {
                return String.valueOf(value);
            }
        }
        Object code = replyPayload.get("code");
        return code == null ? "设备返回失败回执" : "设备返回失败回执, code=" + code;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
