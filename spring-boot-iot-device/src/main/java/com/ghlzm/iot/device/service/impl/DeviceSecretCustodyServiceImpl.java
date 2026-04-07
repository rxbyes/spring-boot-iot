package com.ghlzm.iot.device.service.impl;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.device.dto.DeviceSecretRotateDTO;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.DeviceSecretRotationLog;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.mapper.DeviceMapper;
import com.ghlzm.iot.device.mapper.DeviceSecretRotationLogMapper;
import com.ghlzm.iot.device.service.DeviceSecretCustodyService;
import com.ghlzm.iot.device.service.DeviceService;
import com.ghlzm.iot.device.service.ProductService;
import com.ghlzm.iot.device.vo.DeviceSecretRotateResultVO;
import com.ghlzm.iot.system.entity.AuditLog;
import com.ghlzm.iot.system.security.GovernancePermissionCodes;
import com.ghlzm.iot.system.security.GovernancePermissionGuard;
import com.ghlzm.iot.system.service.AuditLogService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Device secret custody service implementation.
 */
@Service
public class DeviceSecretCustodyServiceImpl implements DeviceSecretCustodyService {

    private final DeviceService deviceService;
    private final ProductService productService;
    private final DeviceMapper deviceMapper;
    private final DeviceSecretRotationLogMapper deviceSecretRotationLogMapper;
    private final GovernancePermissionGuard permissionGuard;
    private final AuditLogService auditLogService;

    public DeviceSecretCustodyServiceImpl(DeviceService deviceService,
                                          ProductService productService,
                                          DeviceMapper deviceMapper,
                                          DeviceSecretRotationLogMapper deviceSecretRotationLogMapper,
                                          GovernancePermissionGuard permissionGuard,
                                          AuditLogService auditLogService) {
        this.deviceService = deviceService;
        this.productService = productService;
        this.deviceMapper = deviceMapper;
        this.deviceSecretRotationLogMapper = deviceSecretRotationLogMapper;
        this.permissionGuard = permissionGuard;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DeviceSecretRotateResultVO rotateDeviceSecret(Long currentUserId,
                                                         Long deviceId,
                                                         Long approverUserId,
                                                         DeviceSecretRotateDTO dto) {
        String nextSecret = normalizeRequired(dto == null ? null : dto.getNewDeviceSecret(), "newDeviceSecret");
        String reason = normalizeOptional(dto == null ? null : dto.getReason());
        permissionGuard.requireDualControl(
                currentUserId,
                approverUserId,
                "device-secret-rotate",
                GovernancePermissionCodes.SECRET_CUSTODY_ROTATE,
                GovernancePermissionCodes.SECRET_CUSTODY_APPROVE
        );
        Device device = deviceService.getRequiredById(currentUserId, deviceId);
        String currentSecret = normalizeOptional(device.getDeviceSecret());
        if (nextSecret.equals(currentSecret)) {
            throw new BizException("新密钥不能与当前密钥一致");
        }

        LocalDateTime rotateTime = LocalDateTime.now();
        String rotationBatchId = buildRotationBatchId(device.getId());
        String productKey = resolveProductKey(device.getProductId());

        device.setDeviceSecret(nextSecret);
        device.setUpdateBy(currentUserId);
        device.setUpdateTime(rotateTime);
        if (deviceMapper.updateById(device) <= 0) {
            throw new BizException("设备密钥轮换失败，请稍后重试");
        }

        DeviceSecretRotationLog rotationLog = new DeviceSecretRotationLog();
        rotationLog.setTenantId(device.getTenantId());
        rotationLog.setDeviceId(device.getId());
        rotationLog.setDeviceCode(device.getDeviceCode());
        rotationLog.setProductKey(productKey);
        rotationLog.setRotationBatchId(rotationBatchId);
        rotationLog.setReason(reason);
        rotationLog.setPreviousSecretDigest(secretDigest(currentSecret));
        rotationLog.setCurrentSecretDigest(secretDigest(nextSecret));
        rotationLog.setRotatedBy(currentUserId);
        rotationLog.setApprovedBy(approverUserId);
        rotationLog.setRotateTime(rotateTime);
        rotationLog.setCreateBy(currentUserId);
        rotationLog.setUpdateBy(currentUserId);
        deviceSecretRotationLogMapper.insert(rotationLog);

        writeAuditLog(device, productKey, currentUserId, approverUserId, rotationBatchId, reason);
        return buildResult(device, productKey, currentUserId, approverUserId, rotationBatchId, rotateTime, reason);
    }

    private DeviceSecretRotateResultVO buildResult(Device device,
                                                   String productKey,
                                                   Long currentUserId,
                                                   Long approverUserId,
                                                   String rotationBatchId,
                                                   LocalDateTime rotateTime,
                                                   String reason) {
        DeviceSecretRotateResultVO result = new DeviceSecretRotateResultVO();
        result.setDeviceId(device.getId());
        result.setDeviceCode(device.getDeviceCode());
        result.setProductKey(productKey);
        result.setRotationBatchId(rotationBatchId);
        result.setRotatedBy(currentUserId);
        result.setApprovedBy(approverUserId);
        result.setRotatedAt(rotateTime);
        result.setReason(reason);
        return result;
    }

    private void writeAuditLog(Device device,
                               String productKey,
                               Long currentUserId,
                               Long approverUserId,
                               String rotationBatchId,
                               String reason) {
        AuditLog auditLog = new AuditLog();
        auditLog.setTenantId(device.getTenantId());
        auditLog.setUserId(currentUserId);
        auditLog.setOperationType("update");
        auditLog.setOperationModule("device-secret-custody");
        auditLog.setOperationMethod("rotateDeviceSecret");
        auditLog.setDeviceCode(device.getDeviceCode());
        auditLog.setProductKey(productKey);
        auditLog.setRequestParams("{\"rotationBatchId\":\"" + rotationBatchId
                + "\",\"approverUserId\":" + approverUserId
                + ",\"reason\":\"" + (reason == null ? "" : reason) + "\"}");
        auditLog.setOperationResult(1);
        auditLog.setResultMessage("rotated");
        auditLog.setOperationTime(new Date());
        auditLogService.addLog(auditLog);
    }

    private String resolveProductKey(Long productId) {
        if (productId == null) {
            return null;
        }
        Product product = productService.getById(productId);
        return product == null ? null : normalizeOptional(product.getProductKey());
    }

    private String buildRotationBatchId(Long deviceId) {
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "ROT-" + (deviceId == null ? "UNKNOWN" : deviceId) + "-" + random;
    }

    private String secretDigest(String secret) {
        if (!StringUtils.hasText(secret)) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(secret.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (Exception ex) {
            throw new BizException("密钥摘要计算失败");
        }
    }

    private String normalizeRequired(String value, String label) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            throw new BizException(label + " 不能为空");
        }
        return normalized;
    }

    private String normalizeOptional(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
