package com.ghlzm.iot.system.service;

import com.ghlzm.iot.system.vo.MenuTreeNodeVO;
import com.ghlzm.iot.system.vo.RoleSummaryVO;
import com.ghlzm.iot.system.vo.UserAuthContextVO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface PermissionService {

    UserAuthContextVO getUserAuthContext(Long userId);

    List<MenuTreeNodeVO> listMenuTree();

    List<Long> listRoleMenuIds(Long roleId);

    void replaceRoleMenus(Long roleId, List<Long> menuIds, Long operatorId);

    List<Long> listUserRoleIds(Long userId);

    Map<Long, List<RoleSummaryVO>> listUserRolesByUserIds(Collection<Long> userIds);

    void replaceUserRoles(Long userId, List<Long> roleIds, Long operatorId);

    void deleteUserRoles(Long userId);

    void deleteRoleRelations(Long roleId);
}
