package com.ghlzm.iot.alarm.controller;

import com.ghlzm.iot.alarm.entity.RiskPoint;
import com.ghlzm.iot.alarm.entity.RiskPointDevice;
import com.ghlzm.iot.alarm.service.RiskPointService;
import com.ghlzm.iot.common.response.R;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 风险点Controller
 */
@RestController
@RequestMapping("/api/risk-point")
public class RiskPointController {

      private final RiskPointService riskPointService;

      public RiskPointController(RiskPointService riskPointService) {
            this.riskPointService = riskPointService;
      }

      /**
       * 新增风险点
       */
      @PostMapping("/add")
      public R<RiskPoint> addRiskPoint(@RequestBody RiskPoint riskPoint) {
            RiskPoint result = riskPointService.addRiskPoint(riskPoint);
            return R.ok(result);
      }

      /**
       * 更新风险点
       */
      @PostMapping("/update")
      public R<RiskPoint> updateRiskPoint(@RequestBody RiskPoint riskPoint) {
            RiskPoint result = riskPointService.updateRiskPoint(riskPoint);
            return R.ok(result);
      }

      /**
       * 删除风险点
       */
      @PostMapping("/delete/{id}")
      public R<Void> deleteRiskPoint(@PathVariable Long id) {
            riskPointService.deleteRiskPoint(id);
            return R.ok();
      }

      /**
       * 根据ID查询风险点
       */
      @GetMapping("/get/{id}")
      public R<RiskPoint> getById(@PathVariable Long id) {
            RiskPoint riskPoint = riskPointService.getById(id);
            return R.ok(riskPoint);
      }

      /**
       * 查询风险点列表
       */
      @GetMapping("/list")
      public R<List<RiskPoint>> listRiskPoints(
                  @RequestParam(required = false) String riskPointCode,
                  @RequestParam(required = false) String riskLevel,
                  @RequestParam(required = false) Integer status) {
            List<RiskPoint> riskPoints = riskPointService.listRiskPoints(riskPointCode, riskLevel, status);
            return R.ok(riskPoints);
      }

      /**
       * 绑定风险点与设备
       */
      @PostMapping("/bind-device")
      public R<Void> bindDevice(@RequestBody RiskPointDevice riskPointDevice) {
            riskPointService.bindDevice(riskPointDevice);
            return R.ok();
      }

      /**
       * 解绑风险点与设备
       */
      @PostMapping("/unbind-device")
      public R<Void> unbindDevice(@RequestParam Long riskPointId, @RequestParam Long deviceId) {
            riskPointService.unbindDevice(riskPointId, deviceId);
            return R.ok();
      }

      /**
       * 查询风险点绑定的设备列表
       */
      @GetMapping("/bound-devices/{riskPointId}")
      public R<List<RiskPointDevice>> listBoundDevices(@PathVariable Long riskPointId) {
            List<RiskPointDevice> devices = riskPointService.listBoundDevices(riskPointId);
            return R.ok(devices);
      }
}
