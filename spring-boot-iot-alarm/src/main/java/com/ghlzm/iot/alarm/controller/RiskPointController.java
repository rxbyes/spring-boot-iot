package com.ghlzm.iot.alarm.controller;

import com.ghlzm.iot.alarm.entity.RiskPoint;
import com.ghlzm.iot.alarm.entity.RiskPointDevice;
import com.ghlzm.iot.alarm.service.RiskPointBindingMaintenanceService;
import com.ghlzm.iot.alarm.service.RiskPointService;
import com.ghlzm.iot.alarm.vo.RiskPointBindingDeviceGroupVO;
import com.ghlzm.iot.alarm.vo.RiskPointBindingSummaryVO;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.device.vo.DeviceOptionVO;
import com.ghlzm.iot.framework.security.JwtUserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 风险点Controller
 */
@RestController
@RequestMapping("/api/risk-point")
public class RiskPointController {

      private final RiskPointService riskPointService;
      private final RiskPointBindingMaintenanceService bindingMaintenanceService;

      public RiskPointController(RiskPointService riskPointService) {
            this(riskPointService, null);
      }

      @Autowired
      public RiskPointController(RiskPointService riskPointService,
                                 RiskPointBindingMaintenanceService bindingMaintenanceService) {
            this.riskPointService = riskPointService;
            this.bindingMaintenanceService = bindingMaintenanceService;
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
                  @RequestParam(required = false) String riskPointLevel,
                  @RequestParam(required = false, name = "riskLevel") String legacyRiskLevel,
                  @RequestParam(required = false) Integer status,
                  Authentication authentication) {
            String normalizedRiskPointLevel = StringUtils.hasText(riskPointLevel) ? riskPointLevel : legacyRiskLevel;
            List<RiskPoint> riskPoints = riskPointService.listRiskPoints(requireCurrentUserId(authentication), riskPointCode, normalizedRiskPointLevel, status);
            return R.ok(riskPoints);
      }

      /**
       * 分页查询风险点列表
       */
      @GetMapping("/page")
      public R<PageResult<RiskPoint>> pageRiskPoints(
                  @RequestParam(required = false) String riskPointCode,
                  @RequestParam(required = false) String riskPointLevel,
                  @RequestParam(required = false, name = "riskLevel") String legacyRiskLevel,
                  @RequestParam(required = false) Integer status,
                  @RequestParam(defaultValue = "1") Long pageNum,
                  @RequestParam(defaultValue = "10") Long pageSize,
                  Authentication authentication) {
            String normalizedRiskPointLevel = StringUtils.hasText(riskPointLevel) ? riskPointLevel : legacyRiskLevel;
            PageResult<RiskPoint> page = riskPointService.pageRiskPoints(requireCurrentUserId(authentication), riskPointCode, normalizedRiskPointLevel, status, pageNum, pageSize);
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

      /**
       * 查询风险点可绑定的设备候选列表
       */
      @GetMapping("/bindable-devices/{riskPointId}")
      public R<List<DeviceOptionVO>> listBindableDevices(@PathVariable Long riskPointId, Authentication authentication) {
            List<DeviceOptionVO> devices = riskPointService.listBindableDevices(riskPointId, requireCurrentUserId(authentication));
            return R.ok(devices);
      }

      /**
       * 查询风险点绑定摘要。
       */
      @GetMapping("/binding-summaries")
      public R<List<RiskPointBindingSummaryVO>> listBindingSummaries(@RequestParam List<Long> riskPointIds,
                                                                     Authentication authentication) {
            List<RiskPointBindingSummaryVO> summaries = bindingMaintenanceService.listBindingSummaries(
                    riskPointIds,
                    requireCurrentUserId(authentication)
            );
            return R.ok(summaries);
      }

      /**
       * 查询风险点按设备分组的正式绑定列表。
       */
      @GetMapping("/binding-groups/{riskPointId}")
      public R<List<RiskPointBindingDeviceGroupVO>> listBindingGroups(@PathVariable Long riskPointId,
                                                                       Authentication authentication) {
            List<RiskPointBindingDeviceGroupVO> groups = bindingMaintenanceService.listBindingGroups(
                    riskPointId,
                    requireCurrentUserId(authentication)
            );
            return R.ok(groups);
      }

      private Long requireCurrentUserId(Authentication authentication) {
            if (authentication == null || !(authentication.getPrincipal() instanceof JwtUserPrincipal principal)) {
                  throw new BizException(401, "未认证，请先登录");
            }
            return principal.userId();
      }
}
