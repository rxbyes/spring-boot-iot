package com.ghlzm.iot.device.controller;

import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.device.service.CockpitService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 驾驶舱控制器，提供风险监测驾驶舱所需的数据接口。
 */
@RestController
@RequestMapping("/api/cockpit")
public class CockpitController {

      private final CockpitService cockpitService;

      public CockpitController(CockpitService cockpitService) {
            this.cockpitService = cockpitService;
      }

      @GetMapping("/data")
      public R<Object> getCockpitData() {
            return R.ok(cockpitService.getCockpitData());
      }

      @GetMapping("/trend")
      public R<Object> getRiskTrendData() {
            return R.ok(cockpitService.getRiskTrendData());
      }

      @GetMapping("/distribution")
      public R<Object> getRiskDistributionData() {
            return R.ok(cockpitService.getRiskDistributionData());
      }

      @GetMapping("/warnings")
      public R<Object> getWarningStatusData() {
            return R.ok(cockpitService.getWarningStatusData());
      }

      @GetMapping("/activities")
      public R<Object> getRecentActivities() {
            return R.ok(cockpitService.getRecentActivities());
      }
}
