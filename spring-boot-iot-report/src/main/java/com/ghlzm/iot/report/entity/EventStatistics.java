package com.ghlzm.iot.report.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * 事件统计实体
 */
@Data
public class EventStatistics implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 日期
     */
    private String date;

    /**
     * 事件数量
     */
    private Integer count;

    /**
     * 不同状态的事件数量
     */
    private Integer pendingCount;
    private Integer processingCount;
    private Integer closedCount;

    /**
     * 平均处置时间（分钟）
     */
    private Double avgProcessingTime;
}
