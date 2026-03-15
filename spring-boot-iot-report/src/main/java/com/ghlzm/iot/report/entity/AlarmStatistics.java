package com.ghlzm.iot.report.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * 告警统计实体
 */
@Data
public class AlarmStatistics implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 日期
     */
    private String date;

    /**
     * 告警数量
     */
    private Integer count;

    /**
     * 不同等级的告警数量
     */
    private Integer criticalCount;
    private Integer warningCount;
    private Integer infoCount;
}
