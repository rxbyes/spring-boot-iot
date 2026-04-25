package com.ghlzm.iot.device.controller;

import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.device.dto.DeviceAddDTO;
import com.ghlzm.iot.device.dto.DeviceBatchAddDTO;
import com.ghlzm.iot.device.dto.DeviceBatchDeleteDTO;
import com.ghlzm.iot.device.dto.DeviceOnboardingBatchActivateDTO;
import com.ghlzm.iot.device.dto.DeviceOnboardingSuggestionQuery;
import com.ghlzm.iot.device.dto.DeviceReplaceDTO;
import com.ghlzm.iot.device.dto.DeviceSecretRotateDTO;
import com.ghlzm.iot.device.service.DeviceOnboardingActivationService;
import com.ghlzm.iot.device.service.DeviceSecretCustodyService;
import com.ghlzm.iot.device.service.DeviceService;
import com.ghlzm.iot.device.vo.DeviceBatchAddResultVO;
import com.ghlzm.iot.device.vo.DeviceDetailVO;
import com.ghlzm.iot.device.vo.DeviceMetricOptionVO;
import com.ghlzm.iot.device.vo.DeviceOnboardingBatchResultVO;
import com.ghlzm.iot.device.vo.DeviceOnboardingSuggestionVO;
import com.ghlzm.iot.device.vo.DeviceOptionVO;
import com.ghlzm.iot.device.vo.DevicePageVO;
import com.ghlzm.iot.device.vo.DeviceReplaceResultVO;
import com.ghlzm.iot.device.vo.DeviceSecretRotateResultVO;
import com.ghlzm.iot.framework.security.JwtUserPrincipal;
import com.ghlzm.iot.system.security.GovernancePermissionCodes;
import com.ghlzm.iot.system.security.GovernancePermissionGuard;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 设备控制器，只负责设备相关的最小建档与查询入口。
 */
@RestController
public class DeviceController {

    private final DeviceService deviceService;
    private final DeviceSecretCustodyService deviceSecretCustodyService;
    private final DeviceOnboardingActivationService deviceOnboardingActivationService;
    private final GovernancePermissionGuard permissionGuard;

    public DeviceController(DeviceService deviceService,
                            DeviceSecretCustodyService deviceSecretCustodyService,
                            DeviceOnboardingActivationService deviceOnboardingActivationService) {
        this(deviceService, deviceSecretCustodyService, deviceOnboardingActivationService, null);
    }

    @Autowired
    public DeviceController(DeviceService deviceService,
                            DeviceSecretCustodyService deviceSecretCustodyService,
                            DeviceOnboardingActivationService deviceOnboardingActivationService,
                            GovernancePermissionGuard permissionGuard) {
        this.deviceService = deviceService;
        this.deviceSecretCustodyService = deviceSecretCustodyService;
        this.deviceOnboardingActivationService = deviceOnboardingActivationService;
        this.permissionGuard = permissionGuard;
    }

    @PostMapping("/api/device/add")
    public R<DeviceDetailVO> add(@RequestBody @Valid DeviceAddDTO dto, Authentication authentication) {
        Long currentUserId = requireCurrentUserId(authentication);
        requirePermission(currentUserId, "新增设备", GovernancePermissionCodes.DEVICE_ADD);
        // 设备创建逻辑放在服务层，控制层仅做参数接收。
        return R.ok(deviceService.addDevice(currentUserId, dto));
    }

    @PostMapping("/api/device/batch-add")
    public R<DeviceBatchAddResultVO> batchAdd(@RequestBody @Valid DeviceBatchAddDTO dto, Authentication authentication) {
        Long currentUserId = requireCurrentUserId(authentication);
        requirePermission(currentUserId, "批量导入设备", GovernancePermissionCodes.DEVICE_IMPORT);
        return R.ok(deviceService.batchAddDevices(currentUserId, dto.getItems()));
    }

    @GetMapping("/api/device/{id}")
    public R<DeviceDetailVO> getById(@PathVariable("id") Long id, Authentication authentication) {
        return R.ok(deviceService.getDetailById(requireCurrentUserId(authentication), id));
    }

    @GetMapping("/api/device/code/{deviceCode}")
    public R<DeviceDetailVO> getByCode(@PathVariable String deviceCode, Authentication authentication) {
        return R.ok(deviceService.getDetailByCode(requireCurrentUserId(authentication), deviceCode));
    }

    @GetMapping("/api/device/page")
    public R<PageResult<DevicePageVO>> page(@RequestParam(required = false) Long deviceId,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) String productKey,
                                            @RequestParam(required = false) String productName,
                                            @RequestParam(required = false) String deviceCode,
                                            @RequestParam(required = false) String deviceName,
                                            @RequestParam(required = false) Integer onlineStatus,
                                            @RequestParam(required = false) Integer activateStatus,
                                            @RequestParam(required = false) Integer deviceStatus,
                                            @RequestParam(required = false) Integer registrationStatus,
                                            @RequestParam(defaultValue = "1") Long pageNum,
                                            @RequestParam(defaultValue = "10") Long pageSize,
                                            Authentication authentication) {
        return R.ok(deviceService.pageDevices(
                requireCurrentUserId(authentication),
                deviceId,
                keyword,
                productKey,
                productName,
                deviceCode,
                deviceName,
                onlineStatus,
                activateStatus,
                deviceStatus,
                registrationStatus,
                pageNum,
                pageSize
        ));
    }

    @GetMapping("/api/device/onboarding/suggestion")
    public R<DeviceOnboardingSuggestionVO> getOnboardingSuggestion(@Valid DeviceOnboardingSuggestionQuery query,
                                                                   Authentication authentication) {
        return R.ok(deviceService.getOnboardingSuggestion(requireCurrentUserId(authentication), query));
    }

    @PostMapping("/api/device/onboarding/batch-activate")
    public R<DeviceOnboardingBatchResultVO> batchActivate(@RequestBody @Valid DeviceOnboardingBatchActivateDTO dto,
                                                          Authentication authentication) {
        Long currentUserId = requireCurrentUserId(authentication);
        requirePermission(currentUserId, "批量转正式设备", GovernancePermissionCodes.DEVICE_IMPORT);
        return R.ok(deviceOnboardingActivationService.activate(currentUserId, dto));
    }

    @PutMapping("/api/device/{id}")
    public R<DeviceDetailVO> update(@PathVariable("id") Long id, @RequestBody @Valid DeviceAddDTO dto, Authentication authentication) {
        Long currentUserId = requireCurrentUserId(authentication);
        requirePermission(currentUserId, "编辑设备", GovernancePermissionCodes.DEVICE_UPDATE);
        return R.ok(deviceService.updateDevice(currentUserId, id, dto));
    }

    @PostMapping("/api/device/{id}/replace")
    public R<DeviceReplaceResultVO> replace(@PathVariable("id") Long id,
                                            @RequestBody @Valid DeviceReplaceDTO dto,
                                            Authentication authentication) {
        Long currentUserId = requireCurrentUserId(authentication);
        requirePermission(currentUserId, "更换设备", GovernancePermissionCodes.DEVICE_REPLACE);
        return R.ok(deviceService.replaceDevice(currentUserId, id, dto));
    }

    @PostMapping("/api/device/{id}/secret-rotate")
    public R<DeviceSecretRotateResultVO> rotateSecret(@PathVariable("id") Long id,
                                                      @RequestBody @Valid DeviceSecretRotateDTO dto,
                                                      @RequestHeader("X-Governance-Approver-Id") Long approverUserId,
                                                      Authentication authentication) {
        Long currentUserId = requireCurrentUserId(authentication);
        requirePermission(currentUserId, "设备密钥轮换", GovernancePermissionCodes.SECRET_CUSTODY_ROTATE);
        return R.ok(deviceSecretCustodyService.rotateDeviceSecret(
                currentUserId,
                id,
                approverUserId,
                dto
        ));
    }

    @DeleteMapping("/api/device/{id}")
    public R<Void> delete(@PathVariable("id") Long id, Authentication authentication) {
        Long currentUserId = requireCurrentUserId(authentication);
        requirePermission(currentUserId, "删除设备", GovernancePermissionCodes.DEVICE_DELETE);
        deviceService.deleteDevice(currentUserId, id);
        return R.ok();
    }

    @PostMapping("/api/device/batch-delete")
    public R<Void> batchDelete(@RequestBody @Valid DeviceBatchDeleteDTO dto, Authentication authentication) {
        Long currentUserId = requireCurrentUserId(authentication);
        requirePermission(currentUserId, "批量删除设备", GovernancePermissionCodes.DEVICE_DELETE);
        deviceService.batchDeleteDevices(currentUserId, dto.getIds());
        return R.ok();
    }

    @GetMapping("/api/device/list")
    public R<List<DeviceOptionVO>> listDeviceOptions(@RequestParam(defaultValue = "false") boolean includeDisabled,
                                                     Authentication authentication) {
        return R.ok(deviceService.listDeviceOptions(requireCurrentUserId(authentication), includeDisabled));
    }

    @GetMapping("/api/device/{deviceId}/metrics")
    public R<List<DeviceMetricOptionVO>> listMetricOptions(@PathVariable Long deviceId, Authentication authentication) {
        return R.ok(deviceService.listMetricOptions(requireCurrentUserId(authentication), deviceId));
    }

    private void requirePermission(Long currentUserId, String actionName, String permissionCode) {
        if (permissionGuard != null) {
            permissionGuard.requireAnyPermission(currentUserId, actionName, permissionCode);
        }
    }

    private Long requireCurrentUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtUserPrincipal principal)) {
            throw new com.ghlzm.iot.common.exception.BizException(401, "未认证，请先登录");
        }
        return principal.userId();
    }
}
