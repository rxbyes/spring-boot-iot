package com.ghlzm.iot.alarm.controller;

import com.ghlzm.iot.alarm.entity.RiskPoint;
import com.ghlzm.iot.alarm.entity.RiskPointDevice;
import com.ghlzm.iot.alarm.service.RiskPointService;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.framework.security.JwtUserPrincipal;
import org.springframework.security.core.Authentication;
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
      public R<RiskPoint> addRiskPoint(@RequestBody RiskPoint riskPoint, Authentication authentication) {
            RiskPoint result = riskPointService.addRiskPoint(riskPoint, requireCurrentUserId(authentication));
            return R.ok(result);
      }

      /**
       * 更新风险点
       */
      @PostMapping("/update")
      public R<RiskPoint> updateRiskPoint(@RequestBody RiskPoint riskPoint, Authentication authentication) {
            RiskPoint result = riskPointService.updateRiskPoint(riskPoint, requireCurrentUserId(authentication));
            return R.ok(result);
      }

      /**
       * 删除风险点
       */
      @PostMapping("/delete/{id}")
      public R<Void> deleteRiskPoint(@PathVariable Long id, Authentication authentication) {
            riskPointService.deleteRiskPoint(id, requireCurrentUserId(authentication));
            return R.ok();
      }

      /**
       * 根据ID查询风险点
       */
      @GetMapping("/get/{id}")
      public R<RiskPoint> getById(@PathVariable Long id, Authentication authentication) {
            RiskPoint riskPoint = riskPointService.getById(id, requireCurrentUserId(authentication));
            return R.ok(riskPoint);
      }

      /**
       * 查询风险点列表
       */
      @GetMapping("/list")
      public R<List<RiskPoint>> listRiskPoints(
                  @RequestParam(required = false) String riskPointCode,
                  @RequestParam(required = false) String riskLevel,
                  @RequestParam(required = false) Integer status,
                  Authentication authentication) {
            List<RiskPoint> riskPoints = riskPointService.listRiskPoints(requireCurrentUserId(authentication), riskPointCode, riskLevel, status);
            return R.ok(riskPoints);
      }

      /**
       * 分页查询风险点列表
       */
      @GetMapping("/page")
      public R<PageResult<RiskPoint>> pageRiskPoints(
                  @RequestParam(required = false) String riskPointCode,
                  @RequestParam(required = false) String riskLevel,
                  @RequestParam(required = false) Integer status,
                  @RequestParam(defaultValue = "1") Long pageNum,
                  @RequestParam(defaultValue = "10") Long pageSize,
                  Authentication authentication) {
            PageResult<RiskPoint> page = riskPointService.pageRiskPoints(requireCurrentUserId(authentication), riskPointCode, riskLevel, status, pageNum, pageSize);
            return R.ok(page);
      }

      /**
       * 绑定风险点与设备
       */
      @PostMapping("/bind-device")
      public R<Void> bindDevice(@RequestBody RiskPointDevice riskPointDevice, Authentication authentication) {
            riskPointService.bindDevice(riskPointDevice, requireCurrentUserId(authentication));
            return R.ok();
      }

      /**
       * 解绑风险点与设备
       */
      @PostMapping("/unbind-device")
      public R<Void> unbindDevice(@RequestParam Long riskPointId, @RequestParam Long deviceId, Authentication authentication) {
            riskPointService.unbindDevice(riskPointId, deviceId, requireCurrentUserId(authentication));
            return R.ok();
      }

      /**
       * 查询风险点绑定的设备列表
       */
      @GetMapping("/bound-devices/{riskPointId}")
      public R<List<RiskPointDevice>> listBoundDevices(@PathVariable Long riskPointId, Authentication authentication) {
            List<RiskPointDevice> devices = riskPointService.listBoundDevices(riskPointId, requireCurrentUserId(authentication));
            return R.ok(devices);
      }

      private Long requireCurrentUserId(Authentication authentication) {
            if (authentication == null || !(authentication.getPrincipal() instanceof JwtUserPrincipal principal)) {
                  throw new BizException(401, "未认证，请先登录");
            }
            return principal.userId();
      }
}
