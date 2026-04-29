package com.ghlzm.iot.device.controller;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.device.dto.DeviceRelationUpsertDTO;
import com.ghlzm.iot.device.service.DeviceRelationService;
import com.ghlzm.iot.device.vo.DeviceRelationVO;
import com.ghlzm.iot.framework.security.JwtUserPrincipal;
import com.ghlzm.iot.system.security.GovernancePermissionCodes;
import com.ghlzm.iot.system.security.GovernancePermissionGuard;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final GovernancePermissionGuard permissionGuard;

    public DeviceRelationController(DeviceRelationService deviceRelationService) {
        this(deviceRelationService, null);
    }

    @Autowired
    public DeviceRelationController(DeviceRelationService deviceRelationService,
                                    GovernancePermissionGuard permissionGuard) {
        this.deviceRelationService = deviceRelationService;
        this.permissionGuard = permissionGuard;
    }

    @GetMapping("/api/device/relations")
    public R<List<DeviceRelationVO>> list(@RequestParam String parentDeviceCode, Authentication authentication) {
        return R.ok(deviceRelationService.listByParentDeviceCode(requireCurrentUserId(authentication), parentDeviceCode));
    }

    @PostMapping("/api/device/relations")
    public R<DeviceRelationVO> add(@RequestBody @Valid DeviceRelationUpsertDTO dto, Authentication authentication) {
        Long currentUserId = requireCurrentUserId(authentication);
        requirePermission(currentUserId, "新增设备关系");
        return R.ok(deviceRelationService.createRelation(currentUserId, dto));
    }

    @PutMapping("/api/device/relations/{relationId}")
    public R<DeviceRelationVO> update(@PathVariable Long relationId,
                                      @RequestBody @Valid DeviceRelationUpsertDTO dto,
                                      Authentication authentication) {
        Long currentUserId = requireCurrentUserId(authentication);
        requirePermission(currentUserId, "编辑设备关系");
        return R.ok(deviceRelationService.updateRelation(currentUserId, relationId, dto));
    }

    @DeleteMapping("/api/device/relations/{relationId}")
    public R<Void> delete(@PathVariable Long relationId, Authentication authentication) {
        Long currentUserId = requireCurrentUserId(authentication);
        requirePermission(currentUserId, "删除设备关系");
        deviceRelationService.deleteRelation(currentUserId, relationId);
        return R.ok();
    }

    private Long requireCurrentUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtUserPrincipal principal)) {
            throw new BizException(401, "未认证，请先登录");
        }
        return principal.userId();
    }

    private void requirePermission(Long currentUserId, String actionName) {
        if (permissionGuard != null) {
            permissionGuard.requireAnyPermission(currentUserId, actionName, GovernancePermissionCodes.DEVICE_UPDATE);
        }
    }
}
