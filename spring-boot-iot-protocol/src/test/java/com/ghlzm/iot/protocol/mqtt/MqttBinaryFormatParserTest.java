package com.ghlzm.iot.protocol.mqtt;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MqttBinaryFormatParserTest {

    private final MqttPayloadFrameParser frameParser = new MqttPayloadFrameParser();
    private final MqttFirmwarePacketParser firmwarePacketParser = new MqttFirmwarePacketParser();

    @Test
    void shouldParseType3FileDescriptorAndBinaryStream() {
        String descriptor = """
                {"did":"device-1","ds_id":"ota-firmware","file_type":"bin","at":"2018-08-02T10:52:32.449Z","desc":"0-2-1024"}
                """.trim();
        byte[] firmwarePacket = buildFirmwarePacket(0, new byte[]{0x11, 0x22, 0x33}, 2, null);
        byte[] payload = buildType3Packet(descriptor, firmwarePacket);

        MqttPayloadFrameParser.ParsedFrame parsedFrame = frameParser.parse("type3", payload);

        assertEquals(MqttDataFormatType.STANDARD_TYPE_3, parsedFrame.dataFormatType());
        assertEquals(descriptor, parsedFrame.jsonMessage());
        assertEquals(firmwarePacket.length, parsedFrame.binaryLength());
        assertArrayEquals(firmwarePacket, parsedFrame.binaryPayload());
    }

    @Test
    void shouldParseFirmwarePacketWithMd5Tail() {
        byte[] packet = buildFirmwarePacket(1, new byte[]{0x01, 0x02, 0x03, 0x04}, 3, "abcd1234");

        MqttFirmwarePacketParser.ParsedFirmwarePacket parsedPacket = firmwarePacketParser.parse("c4", packet);

        assertEquals(1, parsedPacket.packetIndex());
        assertEquals(4, parsedPacket.packetSize());
        assertEquals(3, parsedPacket.totalPackets());
        assertEquals("abcd1234", parsedPacket.firmwareMd5());
        assertArrayEquals(new byte[]{0x01, 0x02, 0x03, 0x04}, parsedPacket.packetData());
    }

    private byte[] buildType3Packet(String descriptor, byte[] binaryPayload) {
        byte[] descriptorBytes = descriptor.getBytes(StandardCharsets.UTF_8);
        int jsonLength = descriptorBytes.length;
        int totalLength = 3 + jsonLength + 2 + binaryPayload.length;
        byte[] packet = new byte[totalLength];
        packet[0] = 0x03;
        packet[1] = (byte) ((jsonLength >> 8) & 0xFF);
        packet[2] = (byte) (jsonLength & 0xFF);
        System.arraycopy(descriptorBytes, 0, packet, 3, jsonLength);
        int binaryLengthIndex = 3 + jsonLength;
        packet[binaryLengthIndex] = (byte) ((binaryPayload.length >> 8) & 0xFF);
        packet[binaryLengthIndex + 1] = (byte) (binaryPayload.length & 0xFF);
        System.arraycopy(binaryPayload, 0, packet, binaryLengthIndex + 2, binaryPayload.length);
        return packet;
    }

    private byte[] buildFirmwarePacket(int packetIndex, byte[] packetData, int totalPackets, String firmwareMd5) {
        byte[] md5Bytes = firmwareMd5 == null ? new byte[0] : firmwareMd5.getBytes(StandardCharsets.UTF_8);
        int totalLength = 6 + packetData.length + 2 + md5Bytes.length;
        byte[] packet = new byte[totalLength];
        packet[0] = (byte) ((packetIndex >> 8) & 0xFF);
        packet[1] = (byte) (packetIndex & 0xFF);
        packet[2] = (byte) ((packetData.length >> 8) & 0xFF);
        packet[3] = (byte) (packetData.length & 0xFF);
        packet[4] = (byte) ((totalPackets >> 8) & 0xFF);
        packet[5] = (byte) (totalPackets & 0xFF);
        System.arraycopy(packetData, 0, packet, 6, packetData.length);
        int md5LengthIndex = 6 + packetData.length;
        packet[md5LengthIndex] = (byte) ((md5Bytes.length >> 8) & 0xFF);
        packet[md5LengthIndex + 1] = (byte) (md5Bytes.length & 0xFF);
        System.arraycopy(md5Bytes, 0, packet, md5LengthIndex + 2, md5Bytes.length);
        return packet;
    }
}
