package com.ghlzm.iot.device.service.impl;

import com.ghlzm.iot.device.service.CockpitService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 驾驶舱服务实现类，提供风险监测驾驶舱所需的数据。
 */
@Service
public class CockpitServiceImpl implements CockpitService {

      @Override
      public Map<String, Object> getCockpitData() {
            Map<String, Object> data = new HashMap<>();

            // 指标数据
            List<Map<String, Object>> metrics = new ArrayList<>();
            metrics.add(createMetric("红色风险", "3", "红色风险需立即处置", "紧急", "danger"));
            metrics.add(createMetric("橙色风险", "8", "橙色风险需重点跟踪", "关注", "warning"));
            metrics.add(createMetric("今日上报", "15", "今日已上报风险数量", "进行中", "brand"));
            metrics.add(createMetric("处置完成", "22", "累计处置完成数量", "成功率", "success"));

            data.put("metrics", metrics);

            // 趋势数据
            List<Map<String, Object>> trendData = new ArrayList<>();
            trendData.add(createTrendData("红色风险", List.of(3, 5, 8, 6, 4, 7, 3)));
            trendData.add(createTrendData("橙色风险", List.of(8, 12, 15, 11, 9, 13, 8)));
            data.put("trendData", trendData);

            // 分布数据
            List<Map<String, Object>> distributionData = new ArrayList<>();
            distributionData.add(createDistributionData(35, "红色风险", "#ff6d6d"));
            distributionData.add(createDistributionData(28, "橙色风险", "#ffb347"));
            distributionData.add(createDistributionData(22, "黄色风险", "#ffd666"));
            distributionData.add(createDistributionData(15, "蓝色风险", "#52aaff"));
            data.put("distributionData", distributionData);

            // 预警状态
            List<Map<String, Object>> warningStatuses = new ArrayList<>();
            warningStatuses.add(createWarningStatus("pending", "待处置", 15, 30, "#ff6d6d", "Warning"));
            warningStatuses.add(createWarningStatus("processing", "处置中", 8, 55, "#ffb347", "Loading"));
            warningStatuses.add(createWarningStatus("completed", "已处置", 22, 100, "#58d377", "Check"));
            warningStatuses.add(createWarningStatus("timeout", "超时预警", 3, 15, "#ff854d", "Timer"));
            data.put("warningStatuses", warningStatuses);

            // 平台能力
            data.put("capabilities", List.of("风险监测", "协议解析", "设备管理", "远程控制", "AI研判", "报告生成"));

            // 最近活动
            List<Map<String, Object>> recentActivities = new ArrayList<>();
            recentActivities.add(createActivity(1, "10:23", "设备#1024 上报温度异常"));
            recentActivities.add(createActivity(2, "09:45", "生成风险分析报告#032"));
            recentActivities.add(createActivity(3, "09:12", "远程控制设备#089"));
            recentActivities.add(createActivity(4, "08:30", "新预警#045 红色等级"));
            data.put("recentActivities", recentActivities);

            return data;
      }

      private Map<String, Object> createMetric(String label, String value, String hint, String badgeLabel,
                  String tone) {
            Map<String, Object> metric = new HashMap<>();
            metric.put("label", label);
            metric.put("value", value);
            metric.put("hint", hint);
            Map<String, Object> badge = new HashMap<>();
            badge.put("label", badgeLabel);
            badge.put("tone", tone);
            metric.put("badge", badge);
            return metric;
      }

      private Map<String, Object> createTrendData(String name, List<Number> data) {
            Map<String, Object> trend = new HashMap<>();
            trend.put("name", name);
            trend.put("data", data);
            return trend;
      }

      private Map<String, Object> createDistributionData(Number value, String name, String color) {
            Map<String, Object> distribution = new HashMap<>();
            distribution.put("value", value);
            distribution.put("name", name);
            distribution.put("color", color);
            return distribution;
      }

      private Map<String, Object> createWarningStatus(String type, String label, Number count, Number percentage,
                  String color, String icon) {
            Map<String, Object> status = new HashMap<>();
            status.put("type", type);
            status.put("label", label);
            status.put("count", count);
            status.put("percentage", percentage);
            status.put("color", color);
            status.put("icon", icon);
            return status;
      }

      private Map<String, Object> createActivity(int id, String time, String desc) {
            Map<String, Object> activity = new HashMap<>();
            activity.put("id", id);
            activity.put("time", time);
            activity.put("desc", desc);
            return activity;
      }

      @Override
      public List<Map<String, Object>> getRiskTrendData() {
            List<Map<String, Object>> trendData = new ArrayList<>();
            trendData.add(createTrendData("红色风险", List.of(3, 5, 8, 6, 4, 7, 3)));
            trendData.add(createTrendData("橙色风险", List.of(8, 12, 15, 11, 9, 13, 8)));
            return trendData;
      }

      @Override
      public List<Map<String, Object>> getRiskDistributionData() {
            List<Map<String, Object>> distributionData = new ArrayList<>();
            distributionData.add(createDistributionData(35, "红色风险", "#ff6d6d"));
            distributionData.add(createDistributionData(28, "橙色风险", "#ffb347"));
            distributionData.add(createDistributionData(22, "黄色风险", "#ffd666"));
            distributionData.add(createDistributionData(15, "蓝色风险", "#52aaff"));
            return distributionData;
      }

      @Override
      public List<Map<String, Object>> getWarningStatusData() {
            List<Map<String, Object>> warningStatuses = new ArrayList<>();
            warningStatuses.add(createWarningStatus("pending", "待处置", 15, 30, "#ff6d6d", "Warning"));
            warningStatuses.add(createWarningStatus("processing", "处置中", 8, 55, "#ffb347", "Loading"));
            warningStatuses.add(createWarningStatus("completed", "已处置", 22, 100, "#58d377", "Check"));
            warningStatuses.add(createWarningStatus("timeout", "超时预警", 3, 15, "#ff854d", "Timer"));
            return warningStatuses;
      }

      @Override
      public List<Map<String, Object>> getRecentActivities() {
            List<Map<String, Object>> recentActivities = new ArrayList<>();
            recentActivities.add(createActivity(1, "10:23", "设备#1024 上报温度异常"));
            recentActivities.add(createActivity(2, "09:45", "生成风险分析报告#032"));
            recentActivities.add(createActivity(3, "09:12", "远程控制设备#089"));
            recentActivities.add(createActivity(4, "08:30", "新预警#045 红色等级"));
            return recentActivities;
      }
}
