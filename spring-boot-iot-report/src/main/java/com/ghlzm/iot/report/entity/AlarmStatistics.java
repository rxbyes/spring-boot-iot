package com.ghlzm.iot.report.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * Alarm statistics and trend point payload.
 */
@Data
public class AlarmStatistics implements Serializable {
    private static final long serialVersionUID = 1L;

    private String date;
    private Integer count;
    private Integer total;
    private Integer alarmCount;
    private Integer eventCount;
    private Integer criticalCount;
    private Integer warningCount;
    private Integer infoCount;
    private Integer critical;
    private Integer high;
    private Integer medium;
    private Integer low;
}
