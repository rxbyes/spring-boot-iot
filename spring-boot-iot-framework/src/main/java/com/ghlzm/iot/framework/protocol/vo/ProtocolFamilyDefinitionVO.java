package com.ghlzm.iot.framework.protocol.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ProtocolFamilyDefinitionVO {

    private Long id;
    private String familyCode;
    private String protocolCode;
    private String displayName;
    private String decryptProfileCode;
    private String signAlgorithm;
    private String normalizationStrategy;
    private String status;
    private Integer versionNo;
    private String publishedStatus;
    private Integer publishedVersionNo;
    private Long approvalOrderId;
    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;
}
