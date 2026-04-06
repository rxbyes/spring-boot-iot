package com.ghlzm.iot.system.security;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.system.service.PermissionService;
import com.ghlzm.iot.system.vo.UserAuthContextVO;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
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
        if (currentUserId == null || currentUserId <= 0) {
            throw new BizException("未登录或登录状态已失效");
        }
        UserAuthContextVO authContext = permissionService.getUserAuthContext(currentUserId);
        if (authContext == null) {
            throw new BizException("未登录或登录状态已失效");
        }
        if (authContext.isSuperAdmin()) {
            return;
        }
        Set<String> expectedCodes = normalizeCodes(permissionCodes);
        if (expectedCodes.isEmpty()) {
            return;
        }
        Set<String> grantedCodes = normalizeCodes(authContext.getPermissions());
        if (grantedCodes.stream().anyMatch(expectedCodes::contains)) {
            return;
        }
        String action = StringUtils.hasText(actionLabel) ? actionLabel.trim() : "当前操作";
        throw new BizException("当前账号缺少“" + action + "”权限");
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

    private Set<String> normalizeCodes(java.util.List<String> codes) {
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
