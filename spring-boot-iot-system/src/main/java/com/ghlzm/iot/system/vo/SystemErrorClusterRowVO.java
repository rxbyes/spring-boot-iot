package com.ghlzm.iot.system.vo;

import lombok.Data;

/**
 * 系统异常聚合行。
 */
@Data
public class SystemErrorClusterRowVO {

    /**
     * 聚合键。
     */
    private String clusterKey;

    /**
     * 异常模块。
     */
    private String operationModule;

    /**
     * 异常类型。
     */
    private String exceptionClass;

    /**
     * 异常编码。
     */
    private String errorCode;

    /**
     * 发生次数。
     */
    private Long count = 0L;

    /**
     * 关联 Trace 去重数。
     */
    private Long distinctTraceCount = 0L;

    /**
     * 关联设备去重数。
     */
    private Long distinctDeviceCount = 0L;

    /**
     * 最近发生时间。
     */
    private String latestOperationTime;

    /**
     * 最近请求目标。
     */
    private String latestRequestUrl;

    /**
     * 最近请求通道。
     */
    private String latestRequestMethod;

    /**
     * 最近异常摘要。
     */
    private String latestResultMessage;
}
