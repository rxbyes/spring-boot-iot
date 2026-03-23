package com.ghlzm.iot.device.service.handler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ghlzm.iot.device.entity.DeviceProperty;
import com.ghlzm.iot.device.entity.ProductModel;
import com.ghlzm.iot.device.mapper.DevicePropertyMapper;
import com.ghlzm.iot.device.mapper.ProductModelMapper;
import com.ghlzm.iot.device.service.CommandRecordService;
import com.ghlzm.iot.device.service.DeviceFileService;
import com.ghlzm.iot.device.service.model.DevicePayloadApplyResult;
import com.ghlzm.iot.device.service.model.DeviceProcessingTarget;
import com.ghlzm.iot.protocol.core.model.DeviceUpMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * payload 处理 stage。
 */
@Component
public class DevicePayloadApplyStageHandler {

    private static final Logger log = LoggerFactory.getLogger(DevicePayloadApplyStageHandler.class);
    private static final List<String> COMMAND_ID_ALIASES = List.of("commandId", "messageId");
    private static final List<String> ERROR_MESSAGE_ALIASES = List.of("errorMessage", "error", "msg", "message");

    private final DevicePropertyMapper devicePropertyMapper;
    private final ProductModelMapper productModelMapper;
    private final CommandRecordService commandRecordService;
    private final DeviceFileService deviceFileService;
    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

    public DevicePayloadApplyStageHandler(DevicePropertyMapper devicePropertyMapper,
                                          ProductModelMapper productModelMapper,
                                          CommandRecordService commandRecordService,
                                          DeviceFileService deviceFileService) {
        this.devicePropertyMapper = devicePropertyMapper;
        this.productModelMapper = productModelMapper;
        this.commandRecordService = commandRecordService;
        this.deviceFileService = deviceFileService;
    }

    public DevicePayloadApplyResult apply(DeviceProcessingTarget target) {
        DeviceUpMessage upMessage = target.getMessage();
        DevicePayloadApplyResult result = new DevicePayloadApplyResult();
        if (isCommandReply(upMessage)) {
            handleCommandReply(target, upMessage);
            result.setBranch("COMMAND_REPLY");
            result.getSummary().put("commandReply", true);
            result.getSummary().put("propertyCount", 0);
            result.getSummary().put("childMessageCount", childMessageCount(upMessage));
            return result;
        }

        if (upMessage.getFilePayload() != null) {
            deviceFileService.handleFilePayload(target.getDevice(), upMessage);
            result.setBranch("FILE_PAYLOAD");
            result.getSummary().put("filePayload", true);
            result.getSummary().put("fileType", upMessage.getFilePayload().getFileType());
            result.getSummary().put("propertyCount", propertyCount(upMessage));
            return result;
        }

        updateLatestProperties(target, upMessage);
        result.setBranch("PROPERTY");
        result.getSummary().put("propertyCount", propertyCount(upMessage));
        result.getSummary().put("childMessageCount", childMessageCount(upMessage));
        return result;
    }

    private void handleCommandReply(DeviceProcessingTarget target, DeviceUpMessage upMessage) {
        Map<String, Object> replyPayload = parseReplyPayload(upMessage.getRawPayload());
        if (replyPayload.isEmpty()) {
            log.warn("设备 ACK 回执无法解析为 JSON, deviceCode={}, topic={}",
                    target.getDevice().getDeviceCode(), upMessage.getTopic());
            return;
        }

        String commandId = resolveCommandId(replyPayload);
        if (!hasText(commandId)) {
            log.warn("设备 ACK 回执缺少 commandId/messageId, deviceCode={}, topic={}",
                    target.getDevice().getDeviceCode(), upMessage.getTopic());
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
                    target.getDevice().getDeviceCode(), commandId, upMessage.getTopic());
        }
    }

    private void updateLatestProperties(DeviceProcessingTarget target, DeviceUpMessage upMessage) {
        Map<String, Object> properties = upMessage.getProperties();
        if (properties == null || properties.isEmpty()) {
            return;
        }

        Map<String, ProductModel> propertyModels = listPropertyModels(target.getDevice().getProductId());
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            String identifier = entry.getKey();
            Object value = entry.getValue();
            ProductModel productModel = propertyModels.get(identifier);

            DeviceProperty property = devicePropertyMapper.selectOne(
                    new LambdaQueryWrapper<DeviceProperty>()
                            .eq(DeviceProperty::getDeviceId, target.getDevice().getId())
                            .eq(DeviceProperty::getIdentifier, identifier)
                            .last("limit 1")
            );

            if (property == null) {
                property = new DeviceProperty();
                property.setTenantId(target.getDevice().getTenantId());
                property.setDeviceId(target.getDevice().getId());
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

    private boolean isCommandReply(DeviceUpMessage upMessage) {
        return upMessage != null && "reply".equalsIgnoreCase(upMessage.getMessageType());
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

    private int propertyCount(DeviceUpMessage upMessage) {
        return upMessage == null || upMessage.getProperties() == null ? 0 : upMessage.getProperties().size();
    }

    private int childMessageCount(DeviceUpMessage upMessage) {
        return upMessage == null || upMessage.getChildMessages() == null ? 0 : upMessage.getChildMessages().size();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
