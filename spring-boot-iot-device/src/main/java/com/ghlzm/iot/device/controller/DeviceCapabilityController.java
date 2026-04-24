package com.ghlzm.iot.device.controller;

import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.device.dto.DeviceCapabilityExecuteDTO;
import com.ghlzm.iot.device.service.DeviceCapabilityService;
import com.ghlzm.iot.device.vo.CommandRecordPageItemVO;
import com.ghlzm.iot.device.vo.DeviceCapabilityExecuteResultVO;
import com.ghlzm.iot.device.vo.DeviceCapabilityOverviewVO;
import com.ghlzm.iot.framework.security.JwtUserPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DeviceCapabilityController {

    private final DeviceCapabilityService deviceCapabilityService;

    public DeviceCapabilityController(DeviceCapabilityService deviceCapabilityService) {
        this.deviceCapabilityService = deviceCapabilityService;
    }

    @GetMapping("/api/device/{deviceCode}/capabilities")
    public R<DeviceCapabilityOverviewVO> getCapabilities(@PathVariable String deviceCode, Authentication authentication) {
        return R.ok(deviceCapabilityService.getCapabilities(requireCurrentUserId(authentication), deviceCode));
    }

    @PostMapping("/api/device/{deviceCode}/capabilities/{capabilityCode}/execute")
    public R<DeviceCapabilityExecuteResultVO> execute(@PathVariable String deviceCode,
                                                      @PathVariable String capabilityCode,
                                                      @RequestBody @Valid DeviceCapabilityExecuteDTO dto,
                                                      Authentication authentication) {
        return R.ok(deviceCapabilityService.execute(requireCurrentUserId(authentication), deviceCode, capabilityCode, dto));
    }

    @GetMapping("/api/device/{deviceCode}/commands")
    public R<PageResult<CommandRecordPageItemVO>> pageCommands(@PathVariable String deviceCode,
                                                               @RequestParam(required = false) String capabilityCode,
                                                               @RequestParam(required = false) String status,
                                                               @RequestParam(defaultValue = "1") Long pageNum,
                                                               @RequestParam(defaultValue = "10") Long pageSize,
                                                               Authentication authentication) {
        return R.ok(deviceCapabilityService.pageCommands(
                requireCurrentUserId(authentication),
                deviceCode,
                capabilityCode,
                status,
                pageNum,
                pageSize
        ));
    }

    private Long requireCurrentUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtUserPrincipal principal)) {
            throw new com.ghlzm.iot.common.exception.BizException(401, "未认证，请先登录");
        }
        return principal.userId();
    }
}
