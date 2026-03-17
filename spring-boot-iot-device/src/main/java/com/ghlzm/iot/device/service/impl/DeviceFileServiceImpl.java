package com.ghlzm.iot.device.service.impl;

import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.service.DeviceFilePayloadListener;
import com.ghlzm.iot.device.service.DeviceFileService;
import com.ghlzm.iot.device.vo.DeviceFileSnapshotVO;
import com.ghlzm.iot.device.vo.DeviceFirmwareAggregateVO;
import com.ghlzm.iot.framework.config.IotProperties;
import com.ghlzm.iot.protocol.core.model.DeviceFilePayload;
import com.ghlzm.iot.protocol.core.model.DeviceFirmwarePacket;
import com.ghlzm.iot.protocol.core.model.DeviceUpMessage;
import lombok.Data;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 设备文件消息服务最小实现。
 * 当前阶段不引入专门文件表，先使用 Redis 保存文件快照和固件分包聚合状态，
 * 同时为后续 OTA 模块预留监听扩展点。
 */
@Service
public class DeviceFileServiceImpl implements DeviceFileService {

    private static final String FILE_KEY_PREFIX = "iot:device:file:";
    private static final String FIRMWARE_KEY_PREFIX = "iot:device:firmware:";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectProvider<List<DeviceFilePayloadListener>> listenersProvider;
    private final IotProperties iotProperties;
    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

    public DeviceFileServiceImpl(StringRedisTemplate stringRedisTemplate,
                                 ObjectProvider<List<DeviceFilePayloadListener>> listenersProvider,
                                 IotProperties iotProperties) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.listenersProvider = listenersProvider;
        this.iotProperties = iotProperties;
    }

    @Override
    public void handleFilePayload(Device device, DeviceUpMessage upMessage) {
        if (device == null || upMessage == null || upMessage.getFilePayload() == null) {
            return;
        }

        DeviceFilePayload filePayload = upMessage.getFilePayload();
        if (filePayload.getFirmwarePacket() != null) {
            saveFirmwarePacket(device, upMessage, filePayload);
        } else {
            saveGenericFileSnapshot(device, upMessage, filePayload);
        }
        notifyListeners(device, upMessage);
    }

    @Override
    public List<DeviceFileSnapshotVO> listFileSnapshots(String deviceCode) {
        if (!hasText(deviceCode)) {
            return List.of();
        }
        return listByPattern(FILE_KEY_PREFIX + deviceCode + ":*", DeviceFileSnapshotVO.class);
    }

    @Override
    public List<DeviceFirmwareAggregateVO> listFirmwareAggregates(String deviceCode) {
        if (!hasText(deviceCode)) {
            return List.of();
        }
        List<FirmwareAggregateRecord> records = listByPattern(FIRMWARE_KEY_PREFIX + deviceCode + ":*", FirmwareAggregateRecord.class);
        List<DeviceFirmwareAggregateVO> aggregates = new ArrayList<>(records.size());
        for (FirmwareAggregateRecord record : records) {
            DeviceFirmwareAggregateVO aggregate = new DeviceFirmwareAggregateVO();
            aggregate.setTransferId(record.getTransferId());
            aggregate.setDeviceCode(record.getDeviceCode());
            aggregate.setProductId(record.getProductId());
            aggregate.setMessageType(record.getMessageType());
            aggregate.setDataSetId(record.getDataSetId());
            aggregate.setFileType(record.getFileType());
            aggregate.setDescription(record.getDescription());
            aggregate.setTimestamp(record.getTimestamp());
            aggregate.setBinaryLength(record.getBinaryLength());
            aggregate.setTotalPackets(record.getTotalPackets());
            aggregate.setReceivedPacketCount(record.getReceivedPacketCount());
            aggregate.setFirmwareMd5(record.getFirmwareMd5());
            aggregate.setCalculatedMd5(record.getCalculatedMd5());
            aggregate.setMd5Matched(record.getMd5Matched());
            aggregate.setCompleted(record.getCompleted());
            aggregate.setAssembledBase64(record.getAssembledBase64());
            aggregate.setAssembledLength(record.getAssembledLength());
            aggregate.setDescriptor(record.getDescriptor());
            aggregate.setUpdatedTime(record.getUpdatedTime());

            List<Integer> indexes = new ArrayList<>(record.getPackets().keySet());
            indexes.sort(Comparator.naturalOrder());
            aggregate.setReceivedPacketIndexes(indexes);
            aggregates.add(aggregate);
        }
        return aggregates;
    }

    private void saveGenericFileSnapshot(Device device, DeviceUpMessage upMessage, DeviceFilePayload filePayload) {
        DeviceFileSnapshotVO snapshot = new DeviceFileSnapshotVO();
        snapshot.setTransferId(resolveTransferId(filePayload));
        snapshot.setDeviceCode(device.getDeviceCode());
        snapshot.setProductId(device.getProductId());
        snapshot.setMessageType(upMessage.getMessageType());
        snapshot.setDataSetId(filePayload.getDataSetId());
        snapshot.setFileType(filePayload.getFileType());
        snapshot.setDescription(filePayload.getDescription());
        snapshot.setTimestamp(filePayload.getTimestamp());
        snapshot.setBinaryLength(filePayload.getBinaryLength());
        snapshot.setBinaryBase64(encode(filePayload.getBinaryPayload()));
        snapshot.setDescriptor(filePayload.getDescriptor());
        snapshot.setCompleted(Boolean.TRUE);
        snapshot.setUpdatedTime(LocalDateTime.now());

        saveJson(buildFileKey(device.getDeviceCode(), filePayload), snapshot);
    }

    private void saveFirmwarePacket(Device device, DeviceUpMessage upMessage, DeviceFilePayload filePayload) {
        String redisKey = buildFirmwareKey(device.getDeviceCode(), filePayload);
        FirmwareAggregateRecord aggregate = readJson(redisKey, FirmwareAggregateRecord.class);
        if (aggregate == null) {
            aggregate = new FirmwareAggregateRecord();
            aggregate.setTransferId(resolveTransferId(filePayload));
            aggregate.setDeviceCode(device.getDeviceCode());
            aggregate.setProductId(device.getProductId());
            aggregate.setDataSetId(filePayload.getDataSetId());
            aggregate.setFileType(filePayload.getFileType());
            aggregate.setDescription(filePayload.getDescription());
            aggregate.setDescriptor(filePayload.getDescriptor());
            aggregate.setPackets(new LinkedHashMap<>());
        }

        DeviceFirmwarePacket firmwarePacket = filePayload.getFirmwarePacket();
        aggregate.setMessageType(upMessage.getMessageType());
        aggregate.setTimestamp(filePayload.getTimestamp());
        aggregate.setBinaryLength(filePayload.getBinaryLength());
        aggregate.setUpdatedTime(LocalDateTime.now());
        if (firmwarePacket.getTotalPackets() != null) {
            aggregate.setTotalPackets(firmwarePacket.getTotalPackets());
        }
        if (firmwarePacket.getFirmwareMd5() != null && !firmwarePacket.getFirmwareMd5().isBlank()) {
            aggregate.setFirmwareMd5(firmwarePacket.getFirmwareMd5());
        }

        if (firmwarePacket.getPacketIndex() != null) {
            aggregate.getPackets().put(firmwarePacket.getPacketIndex(), encode(firmwarePacket.getPacketData()));
        }

        List<Integer> indexes = new ArrayList<>(aggregate.getPackets().keySet());
        indexes.sort(Comparator.naturalOrder());
        aggregate.setReceivedPacketCount(indexes.size());
        aggregate.setReceivedPacketIndexes(indexes);
        aggregate.setCompleted(aggregate.getTotalPackets() != null && indexes.size() >= aggregate.getTotalPackets());

        if (Boolean.TRUE.equals(aggregate.getCompleted())) {
            byte[] assembled = assemblePackets(aggregate.getPackets(), indexes);
            aggregate.setAssembledLength(assembled.length);
            aggregate.setAssembledBase64(encode(assembled));
            if (aggregate.getFirmwareMd5() != null && !aggregate.getFirmwareMd5().isBlank()) {
                String calculatedMd5 = md5Hex(assembled);
                aggregate.setCalculatedMd5(calculatedMd5);
                aggregate.setMd5Matched(calculatedMd5.equalsIgnoreCase(aggregate.getFirmwareMd5()));
            }
        }

        saveJson(redisKey, aggregate);
    }

    private void notifyListeners(Device device, DeviceUpMessage upMessage) {
        List<DeviceFilePayloadListener> listeners = listenersProvider.getIfAvailable(List::of);
        for (DeviceFilePayloadListener listener : listeners) {
            try {
                listener.onFilePayload(device, upMessage);
            } catch (Exception ex) {
                // 扩展监听器失败不影响当前主链路，避免 OTA 等后续模块反向阻塞上报。
            }
        }
    }

    private String buildFileKey(String deviceCode, DeviceFilePayload filePayload) {
        return FILE_KEY_PREFIX + deviceCode + ":" + resolveTransferId(filePayload);
    }

    private String buildFirmwareKey(String deviceCode, DeviceFilePayload filePayload) {
        return FIRMWARE_KEY_PREFIX + deviceCode + ":" + resolveTransferId(filePayload);
    }

    private String resolveTransferId(DeviceFilePayload filePayload) {
        if (filePayload == null) {
            return "unknown";
        }
        if (hasText(filePayload.getDescription())) {
            return normalizeKeyPart(filePayload.getDescription());
        }
        if (hasText(filePayload.getDataSetId())) {
            return normalizeKeyPart(filePayload.getDataSetId());
        }
        if (filePayload.getTimestamp() != null) {
            return filePayload.getTimestamp().toString().replace(":", "-");
        }
        return "default";
    }

    private String normalizeKeyPart(String value) {
        return value.trim().replace(':', '-').replace('/', '_').replace(' ', '_');
    }

    private byte[] assemblePackets(Map<Integer, String> packets, List<Integer> indexes) {
        int totalLength = 0;
        List<byte[]> parts = new ArrayList<>();
        for (Integer index : indexes) {
            byte[] current = decode(packets.get(index));
            parts.add(current);
            totalLength += current.length;
        }
        byte[] assembled = new byte[totalLength];
        int offset = 0;
        for (byte[] part : parts) {
            System.arraycopy(part, 0, assembled, offset, part.length);
            offset += part.length;
        }
        return assembled;
    }

    private String md5Hex(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] md5 = digest.digest(bytes);
            StringBuilder builder = new StringBuilder(md5.length * 2);
            for (byte value : md5) {
                builder.append(String.format(Locale.ROOT, "%02x", value));
            }
            return builder.toString();
        } catch (Exception ex) {
            return null;
        }
    }

    private Duration getFileTtl() {
        Integer hours = iotProperties.getOta() == null || iotProperties.getOta().getFileSessionTtlHours() == null
                ? 24
                : iotProperties.getOta().getFileSessionTtlHours();
        return Duration.ofHours(hours);
    }

    private void saveJson(String key, Object value) {
        try {
            stringRedisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), getFileTtl());
        } catch (JacksonException ex) {
            throw new IllegalStateException("文件消息序列化失败", ex);
        } catch (Exception ex) {
            // Redis 不可用时不阻断当前消息主链路，最少仍保留消息日志和在线状态更新。
        }
    }

    private <T> T readJson(String key, Class<T> type) {
        try {
            String json = stringRedisTemplate.opsForValue().get(key);
            if (!hasText(json)) {
                return null;
            }
            return objectMapper.readValue(json, type);
        } catch (Exception ex) {
            return null;
        }
    }

    private <T> List<T> listByPattern(String pattern, Class<T> type) {
        Set<String> keys;
        try {
            keys = stringRedisTemplate.keys(pattern);
        } catch (Exception ex) {
            return List.of();
        }
        if (keys == null || keys.isEmpty()) {
            return List.of();
        }

        List<T> result = new ArrayList<>();
        for (String key : keys) {
            T current = readJson(key, type);
            if (current != null) {
                result.add(current);
            }
        }
        result.sort((left, right) -> {
            LocalDateTime leftTime = extractUpdatedTime(left);
            LocalDateTime rightTime = extractUpdatedTime(right);
            if (leftTime == null && rightTime == null) {
                return 0;
            }
            if (leftTime == null) {
                return 1;
            }
            if (rightTime == null) {
                return -1;
            }
            return rightTime.compareTo(leftTime);
        });
        return result;
    }

    private LocalDateTime extractUpdatedTime(Object value) {
        if (value instanceof DeviceFileSnapshotVO snapshot) {
            return snapshot.getUpdatedTime();
        }
        if (value instanceof DeviceFirmwareAggregateVO aggregate) {
            return aggregate.getUpdatedTime();
        }
        return null;
    }

    private String encode(byte[] bytes) {
        return bytes == null ? null : Base64.getEncoder().encodeToString(bytes);
    }

    private byte[] decode(String value) {
        return value == null ? new byte[0] : Base64.getDecoder().decode(value.getBytes(StandardCharsets.UTF_8));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    @Data
    public static class FirmwareAggregateRecord extends com.ghlzm.iot.device.vo.DeviceFirmwareAggregateVO {
        private Map<Integer, String> packets = new LinkedHashMap<>();
    }
}
