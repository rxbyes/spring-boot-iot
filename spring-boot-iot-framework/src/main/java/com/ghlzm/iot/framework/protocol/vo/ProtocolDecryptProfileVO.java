package com.ghlzm.iot.framework.protocol.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ProtocolDecryptProfileVO {

    private Long id;
    private String profileCode;
    private String algorithm;
    private String merchantSource;
    private String merchantKey;
    private String transformation;
    private String signatureSecret;
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
