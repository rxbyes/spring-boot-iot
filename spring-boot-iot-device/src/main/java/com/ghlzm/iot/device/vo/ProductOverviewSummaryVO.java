package com.ghlzm.iot.device.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ProductOverviewSummaryVO {

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long productId;

    private String productKey;

    private String productName;

    private String protocolCode;

    private Integer nodeType;

    private String dataFormat;

    private String manufacturer;

    private String description;

    private Integer status;

    private Long deviceCount;

    private Long onlineDeviceCount;

    private LocalDateTime lastReportTime;

    private Integer formalFieldCount;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long latestReleaseBatchId;

    private Integer latestReleasedFieldCount;

    private String latestReleaseStatus;

    private LocalDateTime latestReleaseCreateTime;
}
