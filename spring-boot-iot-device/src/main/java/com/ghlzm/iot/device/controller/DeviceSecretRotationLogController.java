package com.ghlzm.iot.device.controller;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.device.dto.DeviceSecretRotationLogQuery;
import com.ghlzm.iot.device.service.DeviceSecretRotationLogService;
import com.ghlzm.iot.device.vo.DeviceSecretRotationLogPageItemVO;
import com.ghlzm.iot.framework.security.JwtUserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 密钥轮换台账控制器。
 */
@RestController
public class DeviceSecretRotationLogController {

    private final DeviceSecretRotationLogService deviceSecretRotationLogService;

    public DeviceSecretRotationLogController(DeviceSecretRotationLogService deviceSecretRotationLogService) {
        this.deviceSecretRotationLogService = deviceSecretRotationLogService;
    }

    @GetMapping("/api/device/secret-rotation-logs")
    public R<PageResult<DeviceSecretRotationLogPageItemVO>> pageLogs(DeviceSecretRotationLogQuery query,
                                                                     @RequestParam(defaultValue = "1") Integer pageNum,
                                                                     @RequestParam(defaultValue = "10") Integer pageSize,
                                                                     Authentication authentication) {
        return R.ok(deviceSecretRotationLogService.pageLogs(requireCurrentUserId(authentication), query, pageNum, pageSize));
    }

    private Long requireCurrentUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtUserPrincipal principal)) {
            throw new BizException(401, "未认证，请先登录");
        }
        return principal.userId();
    }
}
