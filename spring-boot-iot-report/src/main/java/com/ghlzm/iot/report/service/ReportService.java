package com.ghlzm.iot.report.service;

import com.ghlzm.iot.report.entity.AlarmStatistics;
import com.ghlzm.iot.report.entity.EventStatistics;
import com.ghlzm.iot.report.entity.DeviceHealthStatistics;

import java.util.List;

/**
 * 报表服务接口
 */
public interface ReportService {

    /**
     * 获取风险趋势分析数据
     */
    List<AlarmStatistics> getRiskTrendAnalysis(String startDate, String endDate);

    /**
     * 获取告警统计分析数据
     */
    AlarmStatistics getAlarmStatistics(String startDate, String endDate);

    /**
     * 获取事件闭环分析数据
     */
    EventStatistics getEventClosureAnalysis(String startDate, String endDate);

    /**
     * 获取设备健康分析数据
     */
    DeviceHealthStatistics getDeviceHealthAnalysis();
}
