package com.ghlzm.iot.protocol.mqtt.legacy;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.util.JsonPayloadUtils;
import com.ghlzm.iot.protocol.core.model.DeviceFilePayload;
import com.ghlzm.iot.protocol.core.model.DeviceFirmwarePacket;
import com.ghlzm.iot.protocol.mqtt.MqttDataFormatType;
import com.ghlzm.iot.protocol.mqtt.MqttFirmwarePacketParser;
import com.ghlzm.iot.protocol.mqtt.MqttPayloadDecryptorRegistry;
import com.ghlzm.iot.protocol.mqtt.MqttPayloadFrameParser;
import com.ghlzm.iot.protocol.mqtt.MqttPayloadSecurityValidator;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * `$dp` 外层解包/解密解码器。
 */
@Component
public class LegacyDpEnvelopeDecoder {

    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
    private final MqttPayloadDecryptorRegistry mqttPayloadDecryptorRegistry;
    private final MqttPayloadFrameParser mqttPayloadFrameParser;
    private final MqttPayloadSecurityValidator mqttPayloadSecurityValidator;
    private final MqttFirmwarePacketParser mqttFirmwarePacketParser;

    public LegacyDpEnvelopeDecoder(MqttPayloadDecryptorRegistry mqttPayloadDecryptorRegistry,
                                   MqttPayloadFrameParser mqttPayloadFrameParser,
                                   MqttPayloadSecurityValidator mqttPayloadSecurityValidator,
                                   MqttFirmwarePacketParser mqttFirmwarePacketParser) {
        this.mqttPayloadDecryptorRegistry = mqttPayloadDecryptorRegistry;
        this.mqttPayloadFrameParser = mqttPayloadFrameParser;
        this.mqttPayloadSecurityValidator = mqttPayloadSecurityValidator;
        this.mqttFirmwarePacketParser = mqttFirmwarePacketParser;
    }

    public DecodedEnvelope decode(byte[] payload) throws Exception {
        MqttPayloadFrameParser.ParsedFrame parsedFrame = mqttPayloadFrameParser.parse("mqtt-json", payload);
        String payloadText = sanitizePayload(parsedFrame.jsonMessage());
        Map<String, Object> payloadMap = objectMapper.readValue(payloadText, new TypeReference<>() {
        });

        if (isEncryptedEnvelope(payloadMap)) {
            String appId = extractAppId(payloadMap);
            String encryptedBody = extractEncryptedBody(payloadMap);
            mqttPayloadSecurityValidator.validateEnvelope(appId, payloadMap, encryptedBody);
            MqttPayloadFrameParser.ParsedFrame decryptedFrame = mqttPayloadFrameParser.parse(
                    "mqtt-json-decrypted",
                    mqttPayloadDecryptorRegistry.decryptBytesOrThrow(appId, encryptedBody)
            );
            String plaintext = sanitizePayload(decryptedFrame.jsonMessage());
            Map<String, Object> decryptedMap = objectMapper.readValue(plaintext, new TypeReference<>() {
            });
            enrichByDataFormat(decryptedMap, decryptedFrame);
            return new DecodedEnvelope(
                    decryptedMap,
                    payloadText,
                    decryptedFrame.dataFormatType(),
                    buildFilePayload(decryptedMap, decryptedFrame),
                    appId
            );
        }

        enrichByDataFormat(payloadMap, parsedFrame);
        return new DecodedEnvelope(
                payloadMap,
                buildRawPayloadForLog(parsedFrame, payloadText, payloadMap),
                parsedFrame.dataFormatType(),
                buildFilePayload(payloadMap, parsedFrame),
                null
        );
    }

    private void enrichByDataFormat(Map<String, Object> payloadMap, MqttPayloadFrameParser.ParsedFrame parsedFrame) {
        if (payloadMap == null || parsedFrame == null) {
            return;
        }
        payloadMap.put("_dataFormatType", parsedFrame.dataFormatType().name());
        if (parsedFrame.dataFormatType() == MqttDataFormatType.STANDARD_TYPE_3 && parsedFrame.binaryPayload() != null) {
            payloadMap.put("_binaryLength", parsedFrame.binaryLength());
            payloadMap.put("_fileStreamLength", parsedFrame.binaryLength());
            payloadMap.put("_fileStreamBase64", Base64.getEncoder().encodeToString(parsedFrame.binaryPayload()));
            if (shouldParseFirmwarePacket(payloadMap)) {
                MqttFirmwarePacketParser.ParsedFirmwarePacket packet =
                        mqttFirmwarePacketParser.parse("mqtt-json-firmware", parsedFrame.binaryPayload());
                Map<String, Object> firmwarePacket = new LinkedHashMap<>();
                firmwarePacket.put("packetIndex", packet.packetIndex());
                firmwarePacket.put("packetSize", packet.packetSize());
                firmwarePacket.put("totalPackets", packet.totalPackets());
                firmwarePacket.put("packetDataBase64", Base64.getEncoder().encodeToString(packet.packetData()));
                firmwarePacket.put("md5Length", packet.md5Length());
                firmwarePacket.put("firmwareMd5", packet.firmwareMd5());
                payloadMap.put("_firmwarePacket", firmwarePacket);
            }
        }
    }

    private boolean shouldParseFirmwarePacket(Map<String, Object> payloadMap) {
        String fileType = stringValue(payloadMap.get("file_type"));
        if (fileType != null) {
            String normalized = fileType.trim().toLowerCase();
            if ("bin".equals(normalized) || "firmware".equals(normalized) || "ota".equals(normalized)) {
                return true;
            }
        }
        String dsId = stringValue(payloadMap.get("ds_id"));
        return dsId != null && (dsId.toLowerCase().contains("firmware") || dsId.toLowerCase().contains("ota"));
    }

    private String buildRawPayloadForLog(MqttPayloadFrameParser.ParsedFrame parsedFrame,
                                         String payloadText,
                                         Map<String, Object> payloadMap) throws Exception {
        if (parsedFrame != null
                && parsedFrame.dataFormatType() == MqttDataFormatType.STANDARD_TYPE_3
                && parsedFrame.binaryPayload() != null) {
            Map<String, Object> rawPayload = new LinkedHashMap<>();
            rawPayload.put("dataFormatType", parsedFrame.dataFormatType().name());
            rawPayload.put("descriptor", payloadMap);
            rawPayload.put("binaryLength", parsedFrame.binaryLength());
            rawPayload.put("binaryBase64", Base64.getEncoder().encodeToString(parsedFrame.binaryPayload()));
            return objectMapper.writeValueAsString(rawPayload);
        }
        return payloadText;
    }

    private DeviceFilePayload buildFilePayload(Map<String, Object> payloadMap,
                                               MqttPayloadFrameParser.ParsedFrame parsedFrame) {
        if (payloadMap == null || parsedFrame == null || parsedFrame.dataFormatType() != MqttDataFormatType.STANDARD_TYPE_3) {
            return null;
        }

        DeviceFilePayload filePayload = new DeviceFilePayload();
        filePayload.setDeviceId(stringValue(payloadMap.get("did")));
        filePayload.setDataSetId(stringValue(payloadMap.get("ds_id")));
        filePayload.setFileType(stringValue(payloadMap.get("file_type")));
        filePayload.setDescription(stringValue(payloadMap.get("desc")));
        filePayload.setTimestamp(parseTimestamp(stringValue(payloadMap.get("at"))));
        filePayload.setBinaryLength(parsedFrame.binaryLength());
        filePayload.setBinaryPayload(parsedFrame.safeBinaryPayload());
        filePayload.setDescriptor(new LinkedHashMap<>(payloadMap));

        Object firmwarePacket = payloadMap.get("_firmwarePacket");
        if (firmwarePacket instanceof Map<?, ?> firmwareMap) {
            filePayload.setFirmwarePacket(toFirmwarePacket(firmwareMap));
        }
        return filePayload;
    }

    private DeviceFirmwarePacket toFirmwarePacket(Map<?, ?> firmwareMap) {
        DeviceFirmwarePacket firmwarePacket = new DeviceFirmwarePacket();
        firmwarePacket.setPacketIndex(integerValue(firmwareMap.get("packetIndex")));
        firmwarePacket.setPacketSize(integerValue(firmwareMap.get("packetSize")));
        firmwarePacket.setTotalPackets(integerValue(firmwareMap.get("totalPackets")));
        firmwarePacket.setMd5Length(integerValue(firmwareMap.get("md5Length")));
        firmwarePacket.setFirmwareMd5(stringValue(firmwareMap.get("firmwareMd5")));
        Object packetDataBase64 = firmwareMap.get("packetDataBase64");
        if (packetDataBase64 != null) {
            firmwarePacket.setPacketData(Base64.getDecoder().decode(String.valueOf(packetDataBase64)));
        }
        return firmwarePacket;
    }

    private Integer integerValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String sanitizePayload(String payloadText) {
        return JsonPayloadUtils.normalizeJsonDocument(payloadText);
    }

    private boolean isEncryptedEnvelope(Map<String, Object> payloadMap) {
        return payloadMap.get("header") instanceof Map<?, ?>
                && payloadMap.get("bodies") instanceof Map<?, ?>;
    }

    private String extractAppId(Map<String, Object> payloadMap) {
        Object header = payloadMap.get("header");
        if (header instanceof Map<?, ?> headerMap) {
            Object appId = headerMap.get("appId");
            if (appId != null) {
                return String.valueOf(appId);
            }
        }
        throw new BizException("加密 MQTT 报文缺少 header.appId");
    }

    private String extractEncryptedBody(Map<String, Object> payloadMap) {
        Object bodies = payloadMap.get("bodies");
        if (bodies instanceof Map<?, ?> bodyMap) {
            Object encryptedBody = bodyMap.get("body");
            if (encryptedBody != null) {
                return String.valueOf(encryptedBody);
            }
        }
        throw new BizException("加密 MQTT 报文缺少 bodies.body");
    }

    private LocalDateTime parseTimestamp(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value).atZone(ZoneId.systemDefault()).toLocalDateTime();
        } catch (DateTimeParseException ignored) {
            try {
                long epochMillis = Long.parseLong(value);
                return Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDateTime();
            } catch (NumberFormatException numberFormatException) {
                return null;
            }
        }
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    public record DecodedEnvelope(Map<String, Object> payload,
                                  String rawPayload,
                                  MqttDataFormatType dataFormatType,
                                  DeviceFilePayload filePayload,
                                  String appId) {
    }
}
