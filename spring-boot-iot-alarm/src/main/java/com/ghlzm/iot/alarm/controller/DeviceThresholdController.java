package com.ghlzm.iot.alarm.controller;

import com.ghlzm.iot.alarm.service.DeviceThresholdReadService;
import com.ghlzm.iot.alarm.vo.DeviceThresholdOverviewVO;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.framework.security.JwtUserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DeviceThresholdController {

    private final DeviceThresholdReadService deviceThresholdReadService;

    public DeviceThresholdController(DeviceThresholdReadService deviceThresholdReadService) {
        this.deviceThresholdReadService = deviceThresholdReadService;
    }

    @GetMapping("/api/device/{deviceId}/thresholds")
    public R<DeviceThresholdOverviewVO> getDeviceThresholds(@PathVariable Long deviceId, Authentication authentication) {
        return R.ok(deviceThresholdReadService.getDeviceThresholds(requireCurrentUserId(authentication), deviceId));
    }

    private Long requireCurrentUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtUserPrincipal principal)
                || principal.userId() == null) {
            throw new BizException("当前登录态无效，请重新登录后重试");
        }
        return principal.userId();
    }
}
