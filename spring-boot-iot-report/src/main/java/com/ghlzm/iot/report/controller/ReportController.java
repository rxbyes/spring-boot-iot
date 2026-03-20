package com.ghlzm.iot.report.controller;

import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.report.service.ReportService;
import com.ghlzm.iot.report.entity.AlarmStatistics;
import com.ghlzm.iot.report.entity.EventStatistics;
import com.ghlzm.iot.report.entity.DeviceHealthStatistics;
import org.springframework.web.bind.annotation.*;

import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;

/**
 * 报表控制器
 */
@RestController
@RequestMapping("/api/report")
public class ReportController {

    @Autowired
    private ReportService reportService;

    /**
     * 获取风险趋势分析数据
     */
    @GetMapping("/risk-trend")
    public R<List<AlarmStatistics>> getRiskTrendAnalysis(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        List<AlarmStatistics> data = reportService.getRiskTrendAnalysis(startDate, endDate);
        return R.ok(data);
    }

    /**
     * 获取告警统计分析数据
     */
    @GetMapping("/alarm-statistics")
    public R<AlarmStatistics> getAlarmStatistics(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        AlarmStatistics data = reportService.getAlarmStatistics(startDate, endDate);
        return R.ok(data);
    }

    /**
     * 获取事件闭环分析数据
     */
    @GetMapping("/event-closure")
    public R<EventStatistics> getEventClosureAnalysis(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        EventStatistics data = reportService.getEventClosureAnalysis(startDate, endDate);
        return R.ok(data);
    }

    /**
     * 获取设备健康分析数据
     */
    @GetMapping("/device-health")
    public R<DeviceHealthStatistics> getDeviceHealthAnalysis() {
        DeviceHealthStatistics data = reportService.getDeviceHealthAnalysis();
        return R.ok(data);
    }
}
