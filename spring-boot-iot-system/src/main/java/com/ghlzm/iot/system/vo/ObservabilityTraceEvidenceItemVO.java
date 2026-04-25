package com.ghlzm.iot.system.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ObservabilityTraceEvidenceItemVO {

    private String itemType;
    private Long itemId;
    private String traceId;
    private String code;
    private String name;
    private String domainCode;
    private String objectType;
    private String objectId;
    private String status;
    private Long durationMs;
    private LocalDateTime occurredAt;
}
