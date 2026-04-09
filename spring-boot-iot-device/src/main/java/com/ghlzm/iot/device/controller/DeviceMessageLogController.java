package com.ghlzm.iot.device.controller;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.device.dto.DeviceMessageTraceQuery;
import com.ghlzm.iot.device.entity.DeviceMessageLog;
import com.ghlzm.iot.device.service.DeviceMessageService;
import com.ghlzm.iot.device.vo.DeviceMessageTraceStatsVO;
import com.ghlzm.iot.framework.security.JwtUserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class DeviceMessageLogController {

    private final DeviceMessageService deviceMessageService;

    public DeviceMessageLogController(DeviceMessageService deviceMessageService) {
        this.deviceMessageService = deviceMessageService;
    }

    @GetMapping("/api/device/{deviceCode}/message-logs")
    public R<List<DeviceMessageLog>> getLogs(@PathVariable String deviceCode, Authentication authentication) {
        return R.ok(deviceMessageService.listMessageLogs(requireCurrentUserId(authentication), deviceCode));
    }

    @GetMapping("/api/device/message-trace/page")
    public R<PageResult<DeviceMessageLog>> pageTraceLogs(DeviceMessageTraceQuery query,
                                                         @RequestParam(defaultValue = "1") Integer pageNum,
                                                         @RequestParam(defaultValue = "10") Integer pageSize,
                                                         Authentication authentication) {
        return R.ok(deviceMessageService.pageMessageTraceLogs(requireCurrentUserId(authentication), query, pageNum, pageSize));
    }

    @GetMapping("/api/device/message-trace/stats")
    public R<DeviceMessageTraceStatsVO> getTraceStats(DeviceMessageTraceQuery query, Authentication authentication) {
        return R.ok(deviceMessageService.getMessageTraceStats(requireCurrentUserId(authentication), query));
    }

    private Long requireCurrentUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtUserPrincipal principal)) {
            throw new BizException(401, "未认证，请先登录");
        }
        return principal.userId();
    }
}
