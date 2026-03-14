package com.ghlzm.iot.protocol.core.model;

import java.time.LocalDateTime;
import java.util.Map;
import lombok.Data;

/**
 * 统一文件/固件类上行消息模型。
 * 用于承接表 C.3 文件描述 + 文件流，以及表 C.4 固件分包信息，
 * 让后续 OTA、文件存储等能力可以基于稳定协议对象演进。
 */
@Data
public class DeviceFilePayload {

    /**
     * 设备侧在文件描述中携带的 did，当前先原样保留。
     */
    private String deviceId;

    /**
     * 文件类数据编码，通常对应标准中的 ds_id。
     */
    private String dataSetId;

    /**
     * 文件后缀或文件类型，例如 bin / jpg / png / rtcm。
     */
    private String fileType;

    /**
     * 文件描述中的业务时间。
     */
    private LocalDateTime timestamp;

    /**
     * 文件描述中的分包说明，例如 xx-yy-zz。
     */
    private String description;

    /**
     * 文件流长度。
     */
    private Integer binaryLength;

    /**
     * 文件流原始字节，当前只在协议层透传，不进入一期属性落库。
     */
    private byte[] binaryPayload;

    /**
     * C.4 固件升级分包解析结果。
     */
    private DeviceFirmwarePacket firmwarePacket;

    /**
     * 保留原始描述字段，便于后续增量扩展而不破坏现有模型。
     */
    private Map<String, Object> descriptor;
}
