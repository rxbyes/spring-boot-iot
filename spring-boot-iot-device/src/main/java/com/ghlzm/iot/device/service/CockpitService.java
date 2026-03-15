package com.ghlzm.iot.device.service;

import java.util.List;
import java.util.Map;

/**
 * 驾驶舱服务接口，提供风险监测驾驶舱所需的数据。
 */
public interface CockpitService {

      /**
       * 获取驾驶舱数据
       */
      Map<String, Object> getCockpitData();

      /**
       * 获取风险趋势数据
       */
      List<Map<String, Object>> getRiskTrendData();

      /**
       * 获取风险分布数据
       */
      List<Map<String, Object>> getRiskDistributionData();

      /**
       * 获取预警状态数据
       */
      List<Map<String, Object>> getWarningStatusData();

      /**
       * 获取最近活动
       */
      List<Map<String, Object>> getRecentActivities();
}
