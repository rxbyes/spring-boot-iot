package com.ghlzm.iot.device.vo;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 运行态字段显示规则读模型。
 */
@Data
public class RuntimeMetricDisplayRuleVO {

    private Long id;

    private Long productId;

    private String scopeType;

    private String protocolCode;

    private String scenarioCode;

    private String deviceFamily;

    private String rawIdentifier;

    private String displayName;

    private String unit;

    private String status;

    private Integer versionNo;

    private Long createBy;

    private LocalDateTime createTime;

    private Long updateBy;

    private LocalDateTime updateTime;
}
