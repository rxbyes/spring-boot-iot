package com.ghlzm.iot.device.vo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.Data;

/**
 * 设备固件分包聚合视图对象。
 * 用于调试台查看 C.4 分包聚合、重组和 MD5 校验结果。
 */
@Data
public class DeviceFirmwareAggregateVO {

    private String transferId;
    private String deviceCode;
    private Long productId;
    private String messageType;
    private String dataSetId;
    private String fileType;
    private String description;
    private LocalDateTime timestamp;
    private Integer binaryLength;
    private Integer totalPackets;
    private Integer receivedPacketCount;
    private List<Integer> receivedPacketIndexes;
    private String firmwareMd5;
    private String calculatedMd5;
    private Boolean md5Matched;
    private Boolean completed;
    private String assembledBase64;
    private Integer assembledLength;
    private Map<String, Object> descriptor;
    private LocalDateTime updatedTime;
}
