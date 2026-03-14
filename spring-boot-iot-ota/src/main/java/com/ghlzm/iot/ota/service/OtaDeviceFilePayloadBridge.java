package com.ghlzm.iot.ota.service;

import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.service.DeviceFilePayloadListener;
import com.ghlzm.iot.ota.model.OtaFirmwarePayloadCommand;
import com.ghlzm.iot.protocol.core.model.DeviceFilePayload;
import com.ghlzm.iot.protocol.core.model.DeviceFirmwarePacket;
import com.ghlzm.iot.protocol.core.model.DeviceUpMessage;
import java.util.Base64;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * OTA 模块文件桥接骨架。
 * 当前先把 device 模块传来的统一协议模型转成 OTA 侧命令对象，
 * 但不在这里实现升级任务、包校验状态机或分发逻辑。
 */
@Slf4j
@Service
public class OtaDeviceFilePayloadBridge implements DeviceFilePayloadListener {

    @Override
    public void onFilePayload(Device device, DeviceUpMessage upMessage) {
        if (device == null || upMessage == null || upMessage.getFilePayload() == null) {
            return;
        }

        DeviceFilePayload filePayload = upMessage.getFilePayload();
        if (filePayload.getFirmwarePacket() == null) {
            return;
        }

        OtaFirmwarePayloadCommand command = toCommand(upMessage, filePayload.getFirmwarePacket(), filePayload);
        // 当前模块尚未纳入活跃 reactor，这里只保留桥接和日志占位。
        log.debug("receive firmware payload bridge command: deviceCode={}, packetIndex={}, totalPackets={}",
                command.getDeviceCode(), command.getPacketIndex(), command.getTotalPackets());
    }

    private OtaFirmwarePayloadCommand toCommand(DeviceUpMessage upMessage,
                                                DeviceFirmwarePacket firmwarePacket,
                                                DeviceFilePayload filePayload) {
        OtaFirmwarePayloadCommand command = new OtaFirmwarePayloadCommand();
        command.setDeviceCode(upMessage.getDeviceCode());
        command.setProductKey(upMessage.getProductKey());
        command.setDataSetId(filePayload.getDataSetId());
        command.setFileType(filePayload.getFileType());
        command.setDescription(filePayload.getDescription());
        command.setTimestamp(filePayload.getTimestamp());
        command.setPacketIndex(firmwarePacket.getPacketIndex());
        command.setPacketSize(firmwarePacket.getPacketSize());
        command.setTotalPackets(firmwarePacket.getTotalPackets());
        command.setMd5Length(firmwarePacket.getMd5Length());
        command.setFirmwareMd5(firmwarePacket.getFirmwareMd5());
        command.setPacketDataBase64(firmwarePacket.getPacketData() == null
                ? null
                : Base64.getEncoder().encodeToString(firmwarePacket.getPacketData()));
        return command;
    }
}
