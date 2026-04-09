package com.ghlzm.iot.device.controller;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.device.dto.DeviceAccessErrorQuery;
import com.ghlzm.iot.device.entity.DeviceAccessErrorLog;
import com.ghlzm.iot.device.service.DeviceAccessErrorLogService;
import com.ghlzm.iot.device.vo.DeviceAccessErrorStatsVO;
import com.ghlzm.iot.framework.security.JwtUserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/device/access-error")
public class DeviceAccessErrorLogController {

    private final DeviceAccessErrorLogService deviceAccessErrorLogService;

    public DeviceAccessErrorLogController(DeviceAccessErrorLogService deviceAccessErrorLogService) {
        this.deviceAccessErrorLogService = deviceAccessErrorLogService;
    }

    @GetMapping("/page")
    public R<PageResult<DeviceAccessErrorLog>> pageLogs(DeviceAccessErrorQuery query,
                                                        @RequestParam(defaultValue = "1") Integer pageNum,
                                                        @RequestParam(defaultValue = "10") Integer pageSize,
                                                        Authentication authentication) {
        return R.ok(deviceAccessErrorLogService.pageLogs(requireCurrentUserId(authentication), query, pageNum, pageSize));
    }

    @GetMapping("/stats")
    public R<DeviceAccessErrorStatsVO> getStats(DeviceAccessErrorQuery query, Authentication authentication) {
        return R.ok(deviceAccessErrorLogService.getStats(requireCurrentUserId(authentication), query));
    }

    @GetMapping("/{id}")
    public R<DeviceAccessErrorLog> getById(@PathVariable Long id, Authentication authentication) {
        DeviceAccessErrorLog log = deviceAccessErrorLogService.getById(requireCurrentUserId(authentication), id);
        if (log == null) {
            return R.fail(404, "失败报文归档不存在或已删除");
        }
        return R.ok(log);
    }

    private Long requireCurrentUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtUserPrincipal principal)) {
            throw new BizException(401, "未认证，请先登录");
        }
        return principal.userId();
    }
}
