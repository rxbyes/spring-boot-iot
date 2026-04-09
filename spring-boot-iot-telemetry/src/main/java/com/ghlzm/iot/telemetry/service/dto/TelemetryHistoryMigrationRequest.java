package com.ghlzm.iot.telemetry.service.dto;

import lombok.Data;

/**
 * 历史 TDengine 数据迁移请求。
 */
@Data
public class TelemetryHistoryMigrationRequest {

    private Long deviceId;
    private Long productId;
    private Integer batchSize = 500;
    private Boolean preferLegacy = Boolean.FALSE;
}
