package com.ghlzm.iot.device.controller;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.device.dto.DeviceRelationUpsertDTO;
import com.ghlzm.iot.device.service.DeviceRelationService;
import com.ghlzm.iot.device.vo.DeviceRelationVO;
import com.ghlzm.iot.framework.security.JwtUserPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 设备关系控制器。
 */
@RestController
public class DeviceRelationController {

    private final DeviceRelationService deviceRelationService;

    public DeviceRelationController(DeviceRelationService deviceRelationService) {
        this.deviceRelationService = deviceRelationService;
    }

    @GetMapping("/api/device/relations")
    public R<List<DeviceRelationVO>> list(@RequestParam String parentDeviceCode, Authentication authentication) {
        return R.ok(deviceRelationService.listByParentDeviceCode(requireCurrentUserId(authentication), parentDeviceCode));
    }

    @PostMapping("/api/device/relations")
    public R<DeviceRelationVO> add(@RequestBody @Valid DeviceRelationUpsertDTO dto, Authentication authentication) {
        return R.ok(deviceRelationService.createRelation(requireCurrentUserId(authentication), dto));
    }

    @PutMapping("/api/device/relations/{relationId}")
    public R<DeviceRelationVO> update(@PathVariable Long relationId,
                                      @RequestBody @Valid DeviceRelationUpsertDTO dto,
                                      Authentication authentication) {
        return R.ok(deviceRelationService.updateRelation(requireCurrentUserId(authentication), relationId, dto));
    }

    @DeleteMapping("/api/device/relations/{relationId}")
    public R<Void> delete(@PathVariable Long relationId, Authentication authentication) {
        deviceRelationService.deleteRelation(requireCurrentUserId(authentication), relationId);
        return R.ok();
    }

    private Long requireCurrentUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtUserPrincipal principal)) {
            throw new BizException(401, "未认证，请先登录");
        }
        return principal.userId();
    }
}
