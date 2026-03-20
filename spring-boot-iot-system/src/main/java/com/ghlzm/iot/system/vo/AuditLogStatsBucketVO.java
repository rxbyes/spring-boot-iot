package com.ghlzm.iot.system.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 审计统计分桶结果。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogStatsBucketVO {

    /**
     * 展示标签。
     */
    private String label;

    /**
     * 实际筛选值。
     */
    private String value;

    /**
     * 命中次数。
     */
    private Long count;
}
