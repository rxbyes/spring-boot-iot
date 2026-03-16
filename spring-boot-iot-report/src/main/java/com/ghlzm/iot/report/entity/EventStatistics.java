package com.ghlzm.iot.report.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * Event closure statistics payload.
 */
@Data
public class EventStatistics implements Serializable {
    private static final long serialVersionUID = 1L;

    private String date;
    private Integer count;
    private Integer total;
    private Integer closed;
    private Integer unclosed;
    private Integer pendingCount;
    private Integer processingCount;
    private Integer closedCount;
    private Double avgProcessingTime;
}
