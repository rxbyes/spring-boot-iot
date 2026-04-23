package com.ghlzm.iot.framework.protocol.template.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ProtocolTemplateDefinitionVO {

    private Long id;
    private String templateCode;
    private String familyCode;
    private String protocolCode;
    private String displayName;
    private String expressionJson;
    private String outputMappingJson;
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
