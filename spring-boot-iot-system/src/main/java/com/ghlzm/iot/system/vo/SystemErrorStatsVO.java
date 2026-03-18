package com.ghlzm.iot.system.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 系统异常统计概览。
 */
@Data
public class SystemErrorStatsVO {

    /**
     * 命中总量。
     */
    private Long total = 0L;

    /**
     * 当天命中量。
     */
    private Long todayCount = 0L;

    /**
     * MQTT 异常量。
     */
    private Long mqttCount = 0L;

    /**
     * SYSTEM 异常量。
     */
    private Long systemCount = 0L;

    /**
     * TraceId 去重量。
     */
    private Long distinctTraceCount = 0L;

    /**
     * 设备去重量。
     */
    private Long distinctDeviceCount = 0L;

    /**
     * 高频模块。
     */
    private List<AuditLogStatsBucketVO> topModules = new ArrayList<>();

    /**
     * 高频异常类。
     */
    private List<AuditLogStatsBucketVO> topExceptionClasses = new ArrayList<>();

    /**
     * 高频异常码。
     */
    private List<AuditLogStatsBucketVO> topErrorCodes = new ArrayList<>();
}
