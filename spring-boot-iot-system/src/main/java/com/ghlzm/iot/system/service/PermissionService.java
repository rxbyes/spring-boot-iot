package com.ghlzm.iot.system.service;

import com.ghlzm.iot.system.vo.MenuTreeNodeVO;
import com.ghlzm.iot.system.vo.RoleSummaryVO;
import com.ghlzm.iot.system.vo.UserAuthContextVO;
import com.ghlzm.iot.system.service.model.DataPermissionContext;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface PermissionService {

    UserAuthContextVO getUserAuthContext(Long userId);

    DataPermissionContext getDataPermissionContext(Long userId);

    Set<Long> listAccessibleOrganizationIds(Long userId);

    Set<Long> listWritableOrganizationIds(Long userId);

    List<MenuTreeNodeVO> listMenuTree();

    List<Long> listRoleMenuIds(Long roleId);

    void replaceRoleMenus(Long roleId, List<Long> menuIds, Long operatorId);

    List<Long> listUserRoleIds(Long userId);

    Map<Long, List<RoleSummaryVO>> listUserRolesByUserIds(Collection<Long> userIds);

    void replaceUserRoles(Long userId, List<Long> roleIds, Long operatorId);

    void deleteUserRoles(Long userId);

    void deleteRoleRelations(Long roleId);
}
