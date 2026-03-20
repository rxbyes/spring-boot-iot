package com.ghlzm.iot.protocol.mqtt;

import com.ghlzm.iot.common.exception.BizException;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * 表 C.4 固件升级分包解析器。
 * 当前只负责协议层拆包和字段标准化，不进入 OTA 业务逻辑。
 */
@Component
public class MqttFirmwarePacketParser {

    public ParsedFirmwarePacket parse(String traceId, byte[] payload) {
        if (payload == null || payload.length < 6) {
            throw new BizException(traceId + " 表 C.4 固件分包长度不足");
        }

        ByteBuffer buffer = ByteBuffer.wrap(payload);
        int packetIndex = readUnsignedShort(buffer.get(), buffer.get());
        int packetSize = readUnsignedShort(buffer.get(), buffer.get());
        int totalPackets = readUnsignedShort(buffer.get(), buffer.get());

        if (packetSize < 0) {
            throw new BizException(traceId + " 表 C.4 当前数据包大小非法: " + packetSize);
        }
        if (buffer.remaining() < packetSize) {
            throw new BizException(traceId + " 表 C.4 数据流不完整，预期长度：" + packetSize + "，实际剩余：" + buffer.remaining());
        }

        byte[] packetData = new byte[packetSize];
        buffer.get(packetData, 0, packetSize);

        Integer md5Length = null;
        String firmwareMd5 = null;
        if (buffer.remaining() >= 2) {
            md5Length = readUnsignedShort(buffer.get(), buffer.get());
            if (md5Length > 0) {
                if (buffer.remaining() < md5Length) {
                    throw new BizException(traceId + " 表 C.4 固件 MD5 不完整，预期长度：" + md5Length + "，实际剩余：" + buffer.remaining());
                }
                byte[] md5Bytes = new byte[md5Length];
                buffer.get(md5Bytes, 0, md5Length);
                firmwareMd5 = new String(md5Bytes, StandardCharsets.UTF_8);
            }
        }

        return new ParsedFirmwarePacket(packetIndex, packetSize, totalPackets, packetData, md5Length, firmwareMd5);
    }

    private int readUnsignedShort(byte highByte, byte lowByte) {
        return ((highByte & 0xFF) << 8) | (lowByte & 0xFF);
    }

    public record ParsedFirmwarePacket(int packetIndex,
                                       int packetSize,
                                       int totalPackets,
                                       byte[] packetData,
                                       Integer md5Length,
                                       String firmwareMd5) {
    }
}
