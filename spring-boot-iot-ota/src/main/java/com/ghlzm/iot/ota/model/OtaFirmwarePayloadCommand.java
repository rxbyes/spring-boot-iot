package com.ghlzm.iot.ota.model;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * OTA 模块预留的固件上行命令对象。
 * 当前只做协议桥接承载，不引入真正 OTA 业务状态机。
 */
@Data
public class OtaFirmwarePayloadCommand {

    private String deviceCode;
    private String productKey;
    private String dataSetId;
    private String fileType;
    private String description;
    private LocalDateTime timestamp;
    private Integer packetIndex;
    private Integer packetSize;
    private Integer totalPackets;
    private Integer md5Length;
    private String firmwareMd5;
    private String packetDataBase64;
}
