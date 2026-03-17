package com.ghlzm.iot.message.service.impl;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.device.entity.CommandRecord;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.service.CommandRecordService;
import com.ghlzm.iot.device.service.DeviceService;
import com.ghlzm.iot.device.service.ProductService;
import com.ghlzm.iot.framework.config.IotProperties;
import com.ghlzm.iot.message.mqtt.MqttDownMessagePublisher;
import com.ghlzm.iot.message.service.DownMessageService;
import com.ghlzm.iot.message.service.model.DownMessagePublishCommand;
import com.ghlzm.iot.message.service.model.DownMessagePublishResult;
import com.ghlzm.iot.protocol.core.context.ProtocolContext;
import com.ghlzm.iot.protocol.core.model.DeviceDownMessage;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * MQTT 下行消息服务实现。
 * 该层只负责最小下行发布编排，不实现 ACK、重试和状态机。
 */
@Service
public class DownMessageServiceImpl implements DownMessageService {

    private static final String COMMAND_TYPE_PROPERTY = "property";
    private static final String COMMAND_TYPE_SERVICE = "service";

    private final MqttDownMessagePublisher mqttDownMessagePublisher;
    private final CommandRecordService commandRecordService;
    private final DeviceService deviceService;
    private final ProductService productService;
    private final IotProperties iotProperties;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    public DownMessageServiceImpl(MqttDownMessagePublisher mqttDownMessagePublisher,
                                  CommandRecordService commandRecordService,
                                  DeviceService deviceService,
                                  ProductService productService,
                                  IotProperties iotProperties) {
        this.mqttDownMessagePublisher = mqttDownMessagePublisher;
        this.commandRecordService = commandRecordService;
        this.deviceService = deviceService;
        this.productService = productService;
        this.iotProperties = iotProperties;
    }

    @Override
    public DownMessagePublishResult publish(DownMessagePublishCommand command) {
        if (command == null) {
            throw new BizException("下行发布参数不能为空");
        }

        Device device = deviceService.getRequiredByCode(command.getDeviceCode());
        Product product = productService.getRequiredById(device.getProductId());

        String actualProductKey = hasText(command.getProductKey()) ? command.getProductKey() : product.getProductKey();
        if (!product.getProductKey().equalsIgnoreCase(actualProductKey)) {
            throw new BizException("下行 productKey 与设备所属产品不匹配: " + command.getDeviceCode());
        }

        String actualProtocolCode = hasText(command.getProtocolCode())
                ? command.getProtocolCode()
                : device.getProtocolCode();
        String actualCommandType = hasText(command.getCommandType())
                ? command.getCommandType()
                : inferCommandType(command);
        String actualTopic = hasText(command.getTopic())
                ? command.getTopic()
                : buildRecommendedTopic(actualProductKey, command.getDeviceCode(), actualCommandType, command.getServiceIdentifier());
        int actualQos = command.getQos() == null ? iotProperties.getMqtt().getQos() : command.getQos();
        boolean retained = Boolean.TRUE.equals(command.getRetained());
        String actualCommandId = hasText(command.getMessageId()) ? command.getMessageId() : String.valueOf(System.currentTimeMillis());

        DeviceDownMessage downMessage = new DeviceDownMessage();
        downMessage.setMessageId(actualCommandId);
        downMessage.setCommandType(actualCommandType);
        downMessage.setServiceIdentifier(command.getServiceIdentifier());
        downMessage.setParams(command.getParams());

        ProtocolContext context = new ProtocolContext();
        context.setTenantCode(device.getTenantId() == null ? null : String.valueOf(device.getTenantId()));
        context.setProductKey(actualProductKey);
        context.setDeviceCode(device.getDeviceCode());
        context.setMessageType(actualCommandType);
        context.setTopic(actualTopic);
        context.setClientId(device.getClientId());

        CommandRecord commandRecord = buildCommandRecord(
                device,
                actualProductKey,
                actualTopic,
                actualCommandType,
                command,
                actualCommandId,
                actualQos,
                retained,
                downMessage
        );
        commandRecordService.create(commandRecord);

        try {
            mqttDownMessagePublisher.publish(actualProtocolCode, actualTopic, downMessage, context, actualQos, retained);
            commandRecordService.markSent(commandRecord.getId(), java.time.LocalDateTime.now());
        } catch (RuntimeException ex) {
            commandRecordService.markFailed(commandRecord.getId(), ex.getMessage());
            throw ex;
        }
        return new DownMessagePublishResult(
                actualProtocolCode,
                actualTopic,
                actualQos,
                retained,
                device.getDeviceCode(),
                actualProductKey,
                actualCommandType
        );
    }

    private CommandRecord buildCommandRecord(Device device,
                                             String productKey,
                                             String topic,
                                             String commandType,
                                             DownMessagePublishCommand command,
                                             String commandId,
                                             int qos,
                                             boolean retained,
                                             DeviceDownMessage downMessage) {
        CommandRecord commandRecord = new CommandRecord();
        commandRecord.setTenantId(device.getTenantId());
        commandRecord.setCommandId(commandId);
        commandRecord.setDeviceId(device.getId());
        commandRecord.setDeviceCode(device.getDeviceCode());
        commandRecord.setProductKey(productKey);
        commandRecord.setTopic(topic);
        commandRecord.setCommandType(commandType);
        commandRecord.setServiceIdentifier(command.getServiceIdentifier());
        commandRecord.setRequestPayload(serializeRequestPayload(downMessage));
        commandRecord.setQos(qos);
        commandRecord.setRetained(retained ? 1 : 0);
        return commandRecord;
    }

    private String serializeRequestPayload(DeviceDownMessage downMessage) {
        try {
            return objectMapper.writeValueAsString(downMessage);
        } catch (JacksonException ex) {
            throw new BizException("命令请求报文序列化失败: " + ex.getMessage());
        }
    }

    private String buildRecommendedTopic(String productKey,
                                         String deviceCode,
                                         String commandType,
                                         String serviceIdentifier) {
        if (COMMAND_TYPE_PROPERTY.equalsIgnoreCase(commandType)) {
            return "/sys/" + productKey + "/" + deviceCode + "/thing/property/set";
        }
        if (COMMAND_TYPE_SERVICE.equalsIgnoreCase(commandType)) {
            if (!hasText(serviceIdentifier)) {
                throw new BizException("服务下行缺少 serviceIdentifier");
            }
            return "/sys/" + productKey + "/" + deviceCode + "/thing/service/" + serviceIdentifier + "/invoke";
        }
        throw new BizException("不支持的下行 commandType: " + commandType);
    }

    private String inferCommandType(DownMessagePublishCommand command) {
        if (hasText(command.getServiceIdentifier())) {
            return COMMAND_TYPE_SERVICE;
        }
        return COMMAND_TYPE_PROPERTY;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
