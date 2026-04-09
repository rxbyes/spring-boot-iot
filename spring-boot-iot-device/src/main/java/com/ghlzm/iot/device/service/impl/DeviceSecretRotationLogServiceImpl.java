package com.ghlzm.iot.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.device.dto.DeviceSecretRotationLogQuery;
import com.ghlzm.iot.device.entity.DeviceSecretRotationLog;
import com.ghlzm.iot.device.mapper.DeviceSecretRotationLogMapper;
import com.ghlzm.iot.device.service.DeviceSecretRotationLogService;
import com.ghlzm.iot.device.vo.DeviceSecretRotationLogPageItemVO;
import com.ghlzm.iot.framework.mybatis.PageQueryUtils;
import com.ghlzm.iot.system.security.GovernancePermissionCodes;
import com.ghlzm.iot.system.security.GovernancePermissionGuard;
import com.ghlzm.iot.system.service.PermissionService;
import com.ghlzm.iot.system.vo.UserAuthContextVO;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 密钥轮换台账查询实现。
 */
@Service
public class DeviceSecretRotationLogServiceImpl implements DeviceSecretRotationLogService {

    private final DeviceSecretRotationLogMapper mapper;
    private final GovernancePermissionGuard permissionGuard;
    private final PermissionService permissionService;

    public DeviceSecretRotationLogServiceImpl(DeviceSecretRotationLogMapper mapper,
                                              GovernancePermissionGuard permissionGuard,
                                              PermissionService permissionService) {
        this.mapper = mapper;
        this.permissionGuard = permissionGuard;
        this.permissionService = permissionService;
    }

    @Override
    public PageResult<DeviceSecretRotationLogPageItemVO> pageLogs(Long currentUserId,
                                                                  DeviceSecretRotationLogQuery query,
                                                                  Integer pageNum,
                                                                  Integer pageSize) {
        permissionGuard.requireAnyPermission(
                currentUserId,
                "密钥托管查看",
                GovernancePermissionCodes.SECRET_CUSTODY_VIEW
        );
        UserAuthContextVO authContext = permissionService.getUserAuthContext(currentUserId);
        if (authContext == null) {
            throw new BizException("未登录或登录状态已失效");
        }
        Page<DeviceSecretRotationLog> page = PageQueryUtils.buildPage(pageNum, pageSize);
        Page<DeviceSecretRotationLog> result = mapper.selectPage(page, new LambdaQueryWrapper<DeviceSecretRotationLog>()
                .eq(DeviceSecretRotationLog::getDeleted, 0)
                .eq(!authContext.isSuperAdmin() && authContext.getTenantId() != null,
                        DeviceSecretRotationLog::getTenantId,
                        authContext.getTenantId())
                .eq(StringUtils.hasText(normalize(query == null ? null : query.getDeviceCode())),
                        DeviceSecretRotationLog::getDeviceCode,
                        normalize(query == null ? null : query.getDeviceCode()))
                .eq(StringUtils.hasText(normalize(query == null ? null : query.getProductKey())),
                        DeviceSecretRotationLog::getProductKey,
                        normalize(query == null ? null : query.getProductKey()))
                .eq(StringUtils.hasText(normalize(query == null ? null : query.getRotationBatchId())),
                        DeviceSecretRotationLog::getRotationBatchId,
                        normalize(query == null ? null : query.getRotationBatchId()))
                .eq(query != null && query.getRotatedBy() != null,
                        DeviceSecretRotationLog::getRotatedBy,
                        query == null ? null : query.getRotatedBy())
                .eq(query != null && query.getApprovedBy() != null,
                        DeviceSecretRotationLog::getApprovedBy,
                        query == null ? null : query.getApprovedBy())
                .ge(query != null && query.getBeginTime() != null,
                        DeviceSecretRotationLog::getRotateTime,
                        query == null ? null : query.getBeginTime())
                .le(query != null && query.getEndTime() != null,
                        DeviceSecretRotationLog::getRotateTime,
                        query == null ? null : query.getEndTime())
                .orderByDesc(DeviceSecretRotationLog::getRotateTime)
                .orderByDesc(DeviceSecretRotationLog::getId));
        List<DeviceSecretRotationLogPageItemVO> records = result.getRecords().stream()
                .map(this::toPageItem)
                .toList();
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), records);
    }

    private DeviceSecretRotationLogPageItemVO toPageItem(DeviceSecretRotationLog entity) {
        DeviceSecretRotationLogPageItemVO item = new DeviceSecretRotationLogPageItemVO();
        item.setId(entity.getId());
        item.setDeviceId(entity.getDeviceId());
        item.setDeviceCode(entity.getDeviceCode());
        item.setProductKey(entity.getProductKey());
        item.setRotationBatchId(entity.getRotationBatchId());
        item.setReason(entity.getReason());
        item.setPreviousSecretDigest(entity.getPreviousSecretDigest());
        item.setCurrentSecretDigest(entity.getCurrentSecretDigest());
        item.setRotatedBy(entity.getRotatedBy());
        item.setApprovedBy(entity.getApprovedBy());
        item.setRotateTime(entity.getRotateTime());
        return item;
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
