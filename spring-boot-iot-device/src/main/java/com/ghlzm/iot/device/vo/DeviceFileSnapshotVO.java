package com.ghlzm.iot.device.vo;

import java.time.LocalDateTime;
import java.util.Map;
import lombok.Data;

/**
 * 设备文件快照视图对象。
 * 用于调试台查看 C.3 文件消息在 Redis 中的最小持久化结果。
 */
@Data
public class DeviceFileSnapshotVO {

    private String transferId;
    private String deviceCode;
    private Long productId;
    private String messageType;
    private String dataSetId;
    private String fileType;
    private String description;
    private LocalDateTime timestamp;
    private Integer binaryLength;
    private String binaryBase64;
    private Map<String, Object> descriptor;
    private Boolean completed;
    private LocalDateTime updatedTime;
}
