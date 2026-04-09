package com.ghlzm.iot.system.security;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.system.service.PermissionService;
import com.ghlzm.iot.system.vo.UserAuthContextVO;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 治理关键写操作权限守卫。
 */
@Service
public class GovernancePermissionGuard {

    private final PermissionService permissionService;

    public GovernancePermissionGuard(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    public void requireAnyPermission(Long currentUserId,
                                     String actionLabel,
                                     String... permissionCodes) {
        UserAuthContextVO authContext = requireAuthContext(currentUserId);
        if (authContext.isSuperAdmin()) {
            return;
        }
        Set<String> expectedCodes = normalizeCodes(permissionCodes);
        if (expectedCodes.isEmpty()) {
            return;
        }
        if (hasAnyPermission(authContext, expectedCodes)) {
            return;
        }
        String action = StringUtils.hasText(actionLabel) ? actionLabel.trim() : "当前操作";
        throw new BizException("当前账号缺少“" + action + "”权限");
    }

    public void requireDualControl(Long operatorUserId,
                                   Long approverUserId,
                                   String actionLabel,
                                   String operatorPermissionCode,
                                   String approverPermissionCode,
                                   String... compatiblePermissionCodes) {
        if (approverUserId == null || approverUserId <= 0) {
            throw new BizException("关键操作缺少复核人");
        }
        if (operatorUserId != null && operatorUserId.equals(approverUserId)) {
            throw new BizException("执行人与复核人不能为同一账号");
        }
        String action = StringUtils.hasText(actionLabel) ? actionLabel.trim() : "关键操作";
        requireAnyPermission(
                operatorUserId,
                action,
                mergeCodes(operatorPermissionCode, compatiblePermissionCodes)
        );
        requireAnyPermission(
                approverUserId,
                action + "复核",
                mergeCodes(approverPermissionCode, compatiblePermissionCodes)
        );
    }

    private UserAuthContextVO requireAuthContext(Long userId) {
        if (userId == null || userId <= 0) {
            throw new BizException("未登录或登录状态已失效");
        }
        UserAuthContextVO authContext = permissionService.getUserAuthContext(userId);
        if (authContext == null) {
            throw new BizException("未登录或登录状态已失效");
        }
        return authContext;
    }

    private boolean hasAnyPermission(UserAuthContextVO authContext, Set<String> expectedCodes) {
        if (authContext == null || expectedCodes == null || expectedCodes.isEmpty()) {
            return false;
        }
        Set<String> grantedCodes = normalizeCodes(authContext.getPermissions());
        return grantedCodes.stream().anyMatch(expectedCodes::contains);
    }

    private String[] mergeCodes(String primaryCode, String... compatiblePermissionCodes) {
        List<String> codes = new ArrayList<>();
        if (StringUtils.hasText(primaryCode)) {
            codes.add(primaryCode.trim());
        }
        if (compatiblePermissionCodes != null) {
            for (String code : compatiblePermissionCodes) {
                if (StringUtils.hasText(code)) {
                    codes.add(code.trim());
                }
            }
        }
        return codes.toArray(String[]::new);
    }

    private Set<String> normalizeCodes(String... codes) {
        if (codes == null || codes.length == 0) {
            return Set.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String code : codes) {
            if (StringUtils.hasText(code)) {
                normalized.add(code.trim().toLowerCase(Locale.ROOT));
            }
        }
        return normalized;
    }

    private Set<String> normalizeCodes(List<String> codes) {
        if (CollectionUtils.isEmpty(codes)) {
            return Set.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String code : codes) {
            if (StringUtils.hasText(code)) {
                normalized.add(code.trim().toLowerCase(Locale.ROOT));
            }
        }
        return normalized;
    }
}
