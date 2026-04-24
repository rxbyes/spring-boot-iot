package com.ghlzm.iot.message.service.impl;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.device.capability.DeviceCapabilityCode;
import com.ghlzm.iot.device.capability.DeviceCapabilityDefinition;
import com.ghlzm.iot.device.capability.DeviceCapabilityType;
import com.ghlzm.iot.device.capability.ProductCapabilityMetadata;
import com.ghlzm.iot.device.capability.WarningDeviceKind;
import com.ghlzm.iot.device.capability.VideoDeviceKind;
import com.ghlzm.iot.device.entity.CommandRecord;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.service.CommandRecordService;
import com.ghlzm.iot.device.service.model.DeviceCapabilityCommandRequest;
import com.ghlzm.iot.device.service.model.DeviceCapabilityCommandResult;
import com.ghlzm.iot.framework.config.IotProperties;
import com.ghlzm.iot.message.mqtt.MqttDownMessagePublisher;
import com.ghlzm.iot.message.service.DeviceCapabilityDownCommandService;
import com.ghlzm.iot.message.service.capability.CapabilityCommandPayload;
import com.ghlzm.iot.message.service.capability.WarningCapabilityPayloadBuilder;
import com.ghlzm.iot.message.service.capability.VideoCapabilityPayloadBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class DeviceCapabilityDownCommandServiceImpl implements DeviceCapabilityDownCommandService {

    private static final String COMMAND_TYPE_CAPABILITY = "capability";

    private final MqttDownMessagePublisher mqttDownMessagePublisher;
    private final CommandRecordService commandRecordService;
    private final IotProperties iotProperties;
    private final WarningCapabilityPayloadBuilder warningCapabilityPayloadBuilder;
    private final VideoCapabilityPayloadBuilder videoCapabilityPayloadBuilder;
    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

    public DeviceCapabilityDownCommandServiceImpl(MqttDownMessagePublisher mqttDownMessagePublisher,
                                                  CommandRecordService commandRecordService,
                                                  IotProperties iotProperties,
                                                  WarningCapabilityPayloadBuilder warningCapabilityPayloadBuilder,
                                                  VideoCapabilityPayloadBuilder videoCapabilityPayloadBuilder) {
        this.mqttDownMessagePublisher = mqttDownMessagePublisher;
        this.commandRecordService = commandRecordService;
        this.iotProperties = iotProperties;
        this.warningCapabilityPayloadBuilder = warningCapabilityPayloadBuilder;
        this.videoCapabilityPayloadBuilder = videoCapabilityPayloadBuilder;
    }

    @Override
    public DeviceCapabilityCommandResult execute(DeviceCapabilityCommandRequest request) {
        validateRequest(request);
        validateParams(request);

        String commandId = StringUtils.hasText(request.getCommandId())
                ? request.getCommandId().trim()
                : String.valueOf(System.currentTimeMillis());
        CapabilityCommandPayload payload = buildPayload(request, commandId);

        CommandRecord commandRecord = buildCommandRecord(request, commandId, payload);
        commandRecordService.create(commandRecord);
        try {
            mqttDownMessagePublisher.publishRaw(
                    payload.topic(),
                    payload.payloadBytes(),
                    iotProperties.getMqtt().getQos(),
                    false
            );
            commandRecordService.markSent(commandRecord.getId(), LocalDateTime.now());
        } catch (RuntimeException ex) {
            commandRecordService.markFailed(commandRecord.getId(), ex.getMessage());
            throw ex;
        }

        DeviceCapabilityCommandResult result = new DeviceCapabilityCommandResult();
        result.setCommandId(commandId);
        result.setDeviceCode(request.getDevice().getDeviceCode());
        result.setCapabilityCode(request.getCapability().code());
        result.setStatus("SENT");
        result.setTopic(payload.topic());
        result.setSentAt(LocalDateTime.now());
        return result;
    }

    private void validateRequest(DeviceCapabilityCommandRequest request) {
        if (request == null || request.getDevice() == null || request.getProduct() == null || request.getCapability() == null) {
            throw new BizException("设备能力下发参数不能为空");
        }
        Device device = request.getDevice();
        Product product = request.getProduct();
        DeviceCapabilityDefinition capability = request.getCapability();
        if (!StringUtils.hasText(device.getDeviceCode())) {
            throw new BizException("设备编码不能为空");
        }
        if (!StringUtils.hasText(product.getProductKey())) {
            throw new BizException("产品编码不能为空");
        }
        if (!StringUtils.hasText(capability.code())) {
            throw new BizException("能力编码不能为空");
        }
    }

    private void validateParams(DeviceCapabilityCommandRequest request) {
        Map<String, Map<String, Object>> schema = request.getCapability().paramsSchema();
        Map<String, Object> params = request.getParams() == null ? Map.of() : request.getParams();
        if (schema == null || schema.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Map<String, Object>> entry : schema.entrySet()) {
            String key = entry.getKey();
            Map<String, Object> definition = entry.getValue();
            boolean required = toBoolean(definition.get("required"));
            Object value = params.get(key);
            if (value == null) {
                if (required) {
                    throw new BizException("缺少能力参数: " + key);
                }
                continue;
            }
            String type = string(definition.get("type"));
            if ("integer".equalsIgnoreCase(type)) {
                long numeric = toLong(value, key);
                Object min = definition.get("min");
                Object max = definition.get("max");
                if (min != null && numeric < toLong(min, key)) {
                    throw new BizException("能力参数超出最小值: " + key);
                }
                if (max != null && numeric > toLong(max, key)) {
                    throw new BizException("能力参数超出最大值: " + key);
                }
            } else if ("string".equalsIgnoreCase(type) && !StringUtils.hasText(String.valueOf(value))) {
                throw new BizException("能力参数不能为空: " + key);
            }
        }
    }

    private CapabilityCommandPayload buildPayload(DeviceCapabilityCommandRequest request, String commandId) {
        ProductCapabilityMetadata metadata = request.getMetadata();
        DeviceCapabilityType capabilityType = metadata == null ? DeviceCapabilityType.UNKNOWN : metadata.capabilityType();
        if (capabilityType == DeviceCapabilityType.WARNING) {
            return warningCapabilityPayloadBuilder.build(request, commandId);
        }
        if (capabilityType == DeviceCapabilityType.VIDEO) {
            return videoCapabilityPayloadBuilder.build(request, commandId);
        }
        return buildMonitoringPayload(request, commandId);
    }

    private CapabilityCommandPayload buildMonitoringPayload(DeviceCapabilityCommandRequest request, String commandId) {
        String topic = "/sys/" + request.getProduct().getProductKey() + "/" + request.getDevice().getDeviceCode() + "/thing/service/" + request.getCapability().code() + "/invoke";
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("msgid", commandId);
        payload.put("commandType", COMMAND_TYPE_CAPABILITY);
        payload.put("capabilityCode", request.getCapability().code());
        payload.put("deviceCode", request.getDevice().getDeviceCode());
        payload.put("productKey", request.getProduct().getProductKey());
        payload.put("params", request.getParams());
        return new CapabilityCommandPayload(topic, serialize(payload), COMMAND_TYPE_CAPABILITY, request.getCapability().code(), commandId);
    }

    private CommandRecord buildCommandRecord(DeviceCapabilityCommandRequest request,
                                             String commandId,
                                             CapabilityCommandPayload payload) {
        CommandRecord commandRecord = new CommandRecord();
        commandRecord.setTenantId(request.getDevice().getTenantId());
        commandRecord.setCommandId(commandId);
        commandRecord.setDeviceId(request.getDevice().getId());
        commandRecord.setDeviceCode(request.getDevice().getDeviceCode());
        commandRecord.setProductKey(request.getProduct().getProductKey());
        commandRecord.setTopic(payload.topic());
        commandRecord.setCommandType(payload.commandType());
        commandRecord.setServiceIdentifier(payload.serviceIdentifier());
        commandRecord.setRequestPayload(payload.payloadText());
        commandRecord.setQos(iotProperties.getMqtt().getQos());
        commandRecord.setRetained(0);
        return commandRecord;
    }

    private String serialize(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JacksonException ex) {
            throw new BizException("能力载荷序列化失败: " + ex.getMessage());
        }
    }

    private boolean toBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        return value != null && "true".equalsIgnoreCase(String.valueOf(value).trim());
    }

    private long toLong(Object value, String key) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value).trim());
        } catch (NumberFormatException ex) {
            throw new BizException("能力参数必须为整数: " + key);
        }
    }

    private String string(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
