package com.ghlzm.iot.device.controller;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.device.service.CollectorChildInsightService;
import com.ghlzm.iot.device.vo.CollectorChildInsightOverviewVO;
import com.ghlzm.iot.framework.security.JwtUserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * 采集器子设备总览控制器。
 */
@RestController
public class DeviceCollectorInsightController {

    private final CollectorChildInsightService collectorChildInsightService;

    public DeviceCollectorInsightController(CollectorChildInsightService collectorChildInsightService) {
        this.collectorChildInsightService = collectorChildInsightService;
    }

    @GetMapping("/api/device/{deviceCode}/collector-children/overview")
    public R<CollectorChildInsightOverviewVO> getOverview(@PathVariable String deviceCode,
                                                          Authentication authentication) {
        return R.ok(collectorChildInsightService.getOverview(requireCurrentUserId(authentication), deviceCode));
    }

    private Long requireCurrentUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtUserPrincipal principal)) {
            throw new BizException(401, "未认证，请先登录");
        }
        return principal.userId();
    }
}
