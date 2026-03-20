package com.ghlzm.iot.protocol.mqtt;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.util.JsonPayloadUtils;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * MQTT 设备上报二进制帧解析器。
 * 用于兼容“前 3 字节为格式和长度，后续为 JSON 字符串”的历史设备报文。
 */
@Component
public class MqttPayloadFrameParser {

    /**
     * 解析 MQTT 原始二进制负载。
     * 兼容两种输入：
     * 1. 直接明文 JSON
     * 2. Byte1 类型 + Byte2~3 长度 + Byte4 起 JSON 字符串
     */
    public ParsedFrame parse(String traceId, byte[] data) {
        if (data == null || data.length == 0) {
            throw new BizException(traceId + " MQTT 负载不能为空");
        }

        String directJson = extractDirectJson(data);
        if (directJson != null) {
            return new ParsedFrame(false,
                    MqttDataFormatType.DIRECT_JSON_COMPAT,
                    null,
                    directJson.getBytes(StandardCharsets.UTF_8).length,
                    directJson,
                    0,
                    null);
        }

        if (data.length < 3) {
            throw new BizException(traceId + " MQTT 二进制帧长度不足，至少应包含 3 个字节头");
        }

        ByteBuffer buffer = ByteBuffer.wrap(data);
        byte type = buffer.get();
        byte highByte = buffer.get();
        byte lowByte = buffer.get();
        int length = ((highByte & 0xFF) << 8) | (lowByte & 0xFF);
        MqttDataFormatType dataFormatType = MqttDataFormatType.fromByte(type);

        if (length <= 0) {
            throw new BizException(traceId + " 数据包有效长度非法: " + length);
        }
        if (buffer.remaining() < length) {
            throw new BizException(traceId + " 数据不完整，预期长度：" + length + "，实际剩余：" + buffer.remaining());
        }

        byte[] jsonBytes = new byte[length];
        buffer.get(jsonBytes, 0, length);
        String message = sanitizeJson(new String(jsonBytes, StandardCharsets.UTF_8));
        if (message == null || message.isBlank()) {
            throw new BizException(traceId + " 数据包中 JSON 字符串为空");
        }

        if (dataFormatType == MqttDataFormatType.STANDARD_TYPE_3) {
            if (buffer.remaining() < 2) {
                throw new BizException(traceId + " 表 C.3 文件流长度字段不完整");
            }
            int binaryLength = readUnsignedShort(buffer.get(), buffer.get());
            if (binaryLength < 0) {
                throw new BizException(traceId + " 表 C.3 文件流长度非法: " + binaryLength);
            }
            if (buffer.remaining() < binaryLength) {
                throw new BizException(traceId + " 表 C.3 文件流不完整，预期长度：" + binaryLength + "，实际剩余：" + buffer.remaining());
            }
            byte[] binaryPayload = new byte[binaryLength];
            buffer.get(binaryPayload, 0, binaryLength);
            return new ParsedFrame(true, dataFormatType, type, length, message, binaryLength, binaryPayload);
        }

        return new ParsedFrame(true, dataFormatType, type, length, message, 0, null);
    }

    private int readUnsignedShort(byte highByte, byte lowByte) {
        return ((highByte & 0xFF) << 8) | (lowByte & 0xFF);
    }

    private String extractDirectJson(byte[] data) {
        if (data == null || data.length == 0) {
            return null;
        }

        int startIndex = 0;
        while (startIndex < data.length && isIgnorablePrefixByte(data[startIndex])) {
            startIndex++;
        }
        if (startIndex >= data.length) {
            return null;
        }

        byte firstMeaningfulByte = data[startIndex];
        if (firstMeaningfulByte != '{' && firstMeaningfulByte != '[') {
            return null;
        }
        return JsonPayloadUtils.normalizeJsonDocument(
                new String(data, startIndex, data.length - startIndex, StandardCharsets.UTF_8)
        );
    }

    /**
     * 历史明文报文前面可能会带少量控制字符，例如 0x10。
     * 这里只忽略空白和控制字符，避免把“长度头 + JSON”的二进制帧误判成直接 JSON。
     */
    private boolean isIgnorablePrefixByte(byte current) {
        int unsigned = current & 0xFF;
        return unsigned <= 0x20;
    }

    private String sanitizeJson(String text) {
        return JsonPayloadUtils.normalizeJsonDocument(text);
    }

    /**
     * MQTT 二进制帧解析结果。
     */
    public record ParsedFrame(boolean framed,
                              MqttDataFormatType dataFormatType,
                              Byte dataType,
                              int jsonLength,
                              String jsonMessage,
                              int binaryLength,
                              byte[] binaryPayload) {

        public byte[] safeBinaryPayload() {
            return binaryPayload == null ? null : Arrays.copyOf(binaryPayload, binaryPayload.length);
        }
    }
}
