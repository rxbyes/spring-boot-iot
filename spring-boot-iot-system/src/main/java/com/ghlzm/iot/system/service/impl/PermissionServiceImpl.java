package com.ghlzm.iot.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.system.dto.RoleMenuBindingDTO;
import com.ghlzm.iot.system.dto.UserRoleBindingDTO;
import com.ghlzm.iot.system.dto.UserRoleViewDTO;
import com.ghlzm.iot.system.entity.Menu;
import com.ghlzm.iot.system.entity.Organization;
import com.ghlzm.iot.system.entity.Role;
import com.ghlzm.iot.system.entity.Tenant;
import com.ghlzm.iot.system.entity.User;
import com.ghlzm.iot.system.enums.DataScopeType;
import com.ghlzm.iot.system.mapper.MenuMapper;
import com.ghlzm.iot.system.mapper.OrganizationMapper;
import com.ghlzm.iot.system.mapper.RoleMapper;
import com.ghlzm.iot.system.mapper.RoleMenuMapper;
import com.ghlzm.iot.system.mapper.TenantMapper;
import com.ghlzm.iot.system.mapper.UserMapper;
import com.ghlzm.iot.system.mapper.UserRoleMapper;
import com.ghlzm.iot.system.service.PermissionService;
import com.ghlzm.iot.system.service.model.DataPermissionContext;
import com.ghlzm.iot.system.vo.MenuMetaVO;
import com.ghlzm.iot.system.vo.MenuTreeNodeVO;
import com.ghlzm.iot.system.vo.RoleSummaryVO;
import com.ghlzm.iot.system.vo.UserAuthContextVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PermissionServiceImpl implements PermissionService {

    private static final String BUSINESS_ROLE_CODE = "BUSINESS_STAFF";
    private static final String MANAGEMENT_ROLE_CODE = "MANAGEMENT_STAFF";
    private static final String OPS_ROLE_CODE = "OPS_STAFF";
    private static final String DEVELOPER_ROLE_CODE = "DEVELOPER_STAFF";
    private static final String SUPER_ADMIN_ROLE_CODE = "SUPER_ADMIN";
    private static final String IOT_ACCESS_MENU_CODE = "iot-access";
    private static final String RISK_OPS_MENU_CODE = "risk-ops";
    private static final String SYSTEM_GOVERNANCE_MENU_CODE = "system-governance";
    private static final List<RoleHomePreference> ROLE_HOME_PREFERENCES = List.of(
            new RoleHomePreference(SUPER_ADMIN_ROLE_CODE, SYSTEM_GOVERNANCE_MENU_CODE, "/system-management"),
            new RoleHomePreference(MANAGEMENT_ROLE_CODE, RISK_OPS_MENU_CODE, "/risk-disposal"),
            new RoleHomePreference(BUSINESS_ROLE_CODE, RISK_OPS_MENU_CODE, "/risk-disposal"),
            new RoleHomePreference(OPS_ROLE_CODE, IOT_ACCESS_MENU_CODE, "/device-access"),
            new RoleHomePreference(DEVELOPER_ROLE_CODE, IOT_ACCESS_MENU_CODE, "/device-access")
    );

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final MenuMapper menuMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMenuMapper roleMenuMapper;
    private final TenantMapper tenantMapper;
    private final OrganizationMapper organizationMapper;
    private final ObjectMapper objectMapper;

    public PermissionServiceImpl(UserMapper userMapper,
                                 RoleMapper roleMapper,
                                 MenuMapper menuMapper,
                                 UserRoleMapper userRoleMapper,
                                 RoleMenuMapper roleMenuMapper,
                                 TenantMapper tenantMapper,
                                 OrganizationMapper organizationMapper,
                                 ObjectMapper objectMapper) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.menuMapper = menuMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleMenuMapper = roleMenuMapper;
        this.tenantMapper = tenantMapper;
        this.organizationMapper = organizationMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public UserAuthContextVO getUserAuthContext(Long userId) {
        User user = requireActiveUser(userId);
        List<Role> roles = loadUserRoles(userId);
        boolean superAdmin = roles.stream().anyMatch(role -> SUPER_ADMIN_ROLE_CODE.equalsIgnoreCase(role.getRoleCode()));

        List<Menu> activeMenus = listActiveMenus();
        Map<Long, Menu> menuMap = activeMenus.stream().collect(Collectors.toMap(Menu::getId, item -> item));
        Set<Long> grantedMenuIds;
        if (superAdmin) {
            grantedMenuIds = new LinkedHashSet<>(menuMap.keySet());
        } else {
            List<Long> roleIds = extractRoleIds(roles);
            List<Long> roleMenuIds = CollectionUtils.isEmpty(roleIds)
                    ? Collections.emptyList()
                    : roleMenuMapper.selectMenuIdsByRoleIds(roleIds);
            grantedMenuIds = expandWithAncestors(roleMenuIds, menuMap);
        }

        List<Menu> authorizedMenus = activeMenus.stream()
                .filter(menu -> superAdmin || grantedMenuIds.contains(menu.getId()))
                .sorted(menuComparator())
                .toList();

        List<MenuTreeNodeVO> navigationMenus = buildMenuTree(
                authorizedMenus.stream().filter(menu -> !isButton(menu)).toList()
        );

        UserAuthContextVO context = new UserAuthContextVO();
        context.setUserId(user.getId());
        context.setTenantId(user.getTenantId());
        context.setTenantName(resolveTenantName(user.getTenantId()));
        context.setOrgId(user.getOrgId());
        context.setOrgName(resolveOrganizationName(user.getOrgId()));
        context.setUsername(user.getUsername());
        context.setNickname(user.getNickname());
        context.setRealName(user.getRealName());
        context.setDisplayName(resolveDisplayName(user));
        context.setPhone(user.getPhone());
        context.setEmail(user.getEmail());
        context.setAvatar(user.getAvatar());
        context.setLastLoginTime(user.getLastLoginTime());
        context.setLastLoginIp(user.getLastLoginIp());
        context.setAccountType((superAdmin || Integer.valueOf(1).equals(user.getIsAdmin())) ? "主账号" : "子账号");
        context.setAuthStatus(StringUtils.hasText(user.getRealName()) ? "已填写实名信息" : "未填写实名信息");
        context.setLoginMethods(buildLoginMethods(user));
        DataScopeType scopeType = superAdmin ? DataScopeType.ALL : resolveHighestScope(roles);
        context.setDataScopeType(scopeType.name());
        context.setDataScopeSummary(scopeType.getLabel());
        context.setSuperAdmin(superAdmin);
        context.setHomePath(resolveHomePath(roles, navigationMenus));
        context.setRoles(roles.stream().map(this::toRoleSummary).toList());
        context.setRoleCodes(roles.stream().map(Role::getRoleCode).filter(StringUtils::hasText).toList());
        context.setPermissions(authorizedMenus.stream()
                .filter(this::isButton)
                .map(Menu::getMenuCode)
                .filter(StringUtils::hasText)
                .distinct()
                .toList());
        context.setMenus(navigationMenus);
        return context;
    }

    @Override
    public DataPermissionContext getDataPermissionContext(Long userId) {
        User user = requireActiveUser(userId);
        List<Role> roles = loadUserRoles(userId);
        boolean superAdmin = roles.stream().anyMatch(role -> SUPER_ADMIN_ROLE_CODE.equalsIgnoreCase(role.getRoleCode()));
        DataScopeType scopeType = superAdmin ? DataScopeType.ALL : resolveHighestScope(roles);
        return new DataPermissionContext(user.getId(), user.getTenantId(), user.getOrgId(), scopeType, superAdmin);
    }

    @Override
    public Set<Long> listAccessibleOrganizationIds(Long userId) {
        DataPermissionContext context = getDataPermissionContext(userId);
        if (context.orgId() == null || context.orgId() <= 0) {
            return Set.of();
        }
        return switch (context.dataScopeType()) {
            case ALL, TENANT -> listTenantOrganizationIds(context.tenantId());
            case ORG_AND_CHILDREN -> resolveOrganizationSubtreeIds(context.tenantId(), context.orgId());
            case ORG, SELF -> Set.of(context.orgId());
        };
    }

    @Override
    public List<MenuTreeNodeVO> listMenuTree() {
        return buildMenuTree(listActiveMenus());
    }

    @Override
    public List<Long> listRoleMenuIds(Long roleId) {
        return roleMenuMapper.selectMenuIdsByRoleId(roleId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replaceRoleMenus(Long roleId, List<Long> menuIds, Long operatorId) {
        Role role = roleMapper.selectById(roleId);
        if (role == null || Integer.valueOf(1).equals(role.getDeleted())) {
            throw new BizException("角色不存在");
        }

        List<Menu> activeMenus = listActiveMenus();
        Map<Long, Menu> menuMap = activeMenus.stream().collect(Collectors.toMap(Menu::getId, item -> item));
        if (menuIds != null && menuIds.stream().filter(Objects::nonNull).anyMatch(id -> !menuMap.containsKey(id))) {
            throw new BizException("部分菜单不存在或已禁用");
        }

        Set<Long> normalizedIds = expandWithAncestors(menuIds, menuMap);
        roleMenuMapper.deleteByRoleId(roleId);
        if (normalizedIds.isEmpty()) {
            return;
        }

        List<RoleMenuBindingDTO> bindings = normalizedIds.stream()
                .map(menuId -> new RoleMenuBindingDTO(IdWorker.getId(), roleId, menuId))
                .toList();
        roleMenuMapper.batchInsert(bindings);
    }

    @Override
    public List<Long> listUserRoleIds(Long userId) {
        return userRoleMapper.selectRoleIdsByUserId(userId);
    }

    @Override
    public Map<Long, List<RoleSummaryVO>> listUserRolesByUserIds(Collection<Long> userIds) {
        if (CollectionUtils.isEmpty(userIds)) {
            return Collections.emptyMap();
        }

        return userRoleMapper.selectRoleViewsByUserIds(userIds).stream()
                .collect(Collectors.groupingBy(
                        UserRoleViewDTO::getUserId,
                        LinkedHashMap::new,
                        Collectors.mapping(this::toRoleSummary, Collectors.toList())
                ));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replaceUserRoles(Long userId, List<Long> roleIds, Long operatorId) {
        List<Long> distinctRoleIds = roleIds == null
                ? Collections.emptyList()
                : roleIds.stream().filter(Objects::nonNull).distinct().toList();

        if (!distinctRoleIds.isEmpty()) {
            List<Role> roles = loadActiveRolesByIds(distinctRoleIds);
            if (roles.size() != distinctRoleIds.size()) {
                throw new BizException("部分角色不存在或已禁用");
            }
        }

        userRoleMapper.deleteByUserId(userId);
        if (distinctRoleIds.isEmpty()) {
            return;
        }

        List<UserRoleBindingDTO> bindings = distinctRoleIds.stream()
                .map(roleId -> new UserRoleBindingDTO(IdWorker.getId(), userId, roleId))
                .toList();
        userRoleMapper.batchInsert(bindings);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUserRoles(Long userId) {
        userRoleMapper.deleteByUserId(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRoleRelations(Long roleId) {
        roleMenuMapper.deleteByRoleId(roleId);
        userRoleMapper.deleteByRoleId(roleId);
    }

    private List<Role> loadActiveRolesByIds(List<Long> roleIds) {
        if (CollectionUtils.isEmpty(roleIds)) {
            return Collections.emptyList();
        }

        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Role::getId, roleIds)
                .eq(Role::getStatus, 1)
                .orderByAsc(Role::getCreateTime)
                .orderByAsc(Role::getId);
        return roleMapper.selectList(wrapper);
    }

    private User requireActiveUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || Integer.valueOf(1).equals(user.getDeleted())) {
            throw new BizException(401, "用户不存在或已失效");
        }
        return user;
    }

    private List<Role> loadUserRoles(Long userId) {
        return loadActiveRolesByIds(userRoleMapper.selectRoleIdsByUserId(userId));
    }

    private List<Menu> listActiveMenus() {
        LambdaQueryWrapper<Menu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Menu::getStatus, 1)
                .orderByAsc(Menu::getSort)
                .orderByAsc(Menu::getId);
        return menuMapper.selectList(wrapper);
    }

    private List<Long> extractRoleIds(List<Role> roles) {
        return roles.stream().map(Role::getId).filter(Objects::nonNull).toList();
    }

    private List<String> buildLoginMethods(User user) {
        List<String> methods = new ArrayList<>();
        methods.add("账号登录");
        if (StringUtils.hasText(user.getPhone())) {
            methods.add("手机号登录");
        }
        return methods;
    }

    private String resolveTenantName(Long tenantId) {
        if (tenantId == null) {
            return null;
        }
        Tenant tenant = tenantMapper.selectById(tenantId);
        return tenant == null ? null : tenant.getTenantName();
    }

    private String resolveOrganizationName(Long orgId) {
        if (orgId == null) {
            return null;
        }
        Organization organization = organizationMapper.selectById(orgId);
        return organization == null ? null : organization.getOrgName();
    }

    private String resolveDisplayName(User user) {
        if (StringUtils.hasText(user.getNickname())) {
            return user.getNickname();
        }
        if (StringUtils.hasText(user.getRealName())) {
            return user.getRealName();
        }
        return user.getUsername();
    }

    private DataScopeType resolveHighestScope(List<Role> roles) {
        if (CollectionUtils.isEmpty(roles)) {
            return DataScopeType.SELF;
        }
        DataScopeType scopeType = DataScopeType.SELF;
        for (Role role : roles) {
            scopeType = DataScopeType.pickHigher(scopeType, DataScopeType.fromCode(role.getDataScopeType()));
        }
        return scopeType;
    }

    private Set<Long> listTenantOrganizationIds(Long tenantId) {
        return organizationMapper.selectList(new LambdaQueryWrapper<Organization>()
                        .select(Organization::getId, Organization::getParentId)
                        .eq(tenantId != null, Organization::getTenantId, tenantId)
                        .eq(Organization::getDeleted, 0))
                .stream()
                .map(Organization::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<Long> resolveOrganizationSubtreeIds(Long tenantId, Long rootOrgId) {
        List<Organization> organizations = organizationMapper.selectList(new LambdaQueryWrapper<Organization>()
                .select(Organization::getId, Organization::getParentId)
                .eq(tenantId != null, Organization::getTenantId, tenantId)
                .eq(Organization::getDeleted, 0)
                .orderByAsc(Organization::getId));
        if (CollectionUtils.isEmpty(organizations)) {
            return Set.of(rootOrgId);
        }
        Map<Long, List<Long>> childrenMap = organizations.stream()
                .filter(item -> item.getId() != null)
                .collect(Collectors.groupingBy(
                        item -> item.getParentId() == null ? 0L : item.getParentId(),
                        LinkedHashMap::new,
                        Collectors.mapping(Organization::getId, Collectors.toList())
                ));
        LinkedHashSet<Long> scopedIds = new LinkedHashSet<>();
        collectOrganizationChildren(rootOrgId, childrenMap, scopedIds);
        return scopedIds.isEmpty() ? Set.of(rootOrgId) : scopedIds;
    }

    private void collectOrganizationChildren(Long currentOrgId, Map<Long, List<Long>> childrenMap, Set<Long> scopedIds) {
        if (currentOrgId == null || !scopedIds.add(currentOrgId)) {
            return;
        }
        for (Long childId : childrenMap.getOrDefault(currentOrgId, List.of())) {
            collectOrganizationChildren(childId, childrenMap, scopedIds);
        }
    }

    private Set<Long> expandWithAncestors(Collection<Long> menuIds, Map<Long, Menu> menuMap) {
        if (CollectionUtils.isEmpty(menuIds)) {
            return new LinkedHashSet<>();
        }

        Set<Long> normalizedIds = new LinkedHashSet<>();
        for (Long menuId : menuIds) {
            if (menuId == null || !menuMap.containsKey(menuId)) {
                continue;
            }
            Menu current = menuMap.get(menuId);
            while (current != null && current.getId() != null && normalizedIds.add(current.getId())) {
                Long parentId = current.getParentId();
                if (parentId == null || parentId <= 0) {
                    break;
                }
                current = menuMap.get(parentId);
            }
        }
        return normalizedIds;
    }

    private List<MenuTreeNodeVO> buildMenuTree(List<Menu> menus) {
        Map<Long, MenuTreeNodeVO> nodeMap = new LinkedHashMap<>();
        List<MenuTreeNodeVO> roots = new ArrayList<>();

        menus.stream().sorted(menuComparator()).forEach(menu -> nodeMap.put(menu.getId(), toMenuNode(menu)));
        for (Menu menu : menus.stream().sorted(menuComparator()).toList()) {
            MenuTreeNodeVO node = nodeMap.get(menu.getId());
            Long parentId = menu.getParentId();
            if (parentId == null || parentId <= 0 || !nodeMap.containsKey(parentId)) {
                roots.add(node);
                continue;
            }
            nodeMap.get(parentId).getChildren().add(node);
        }

        sortNodes(roots);
        return roots;
    }

    private void sortNodes(List<MenuTreeNodeVO> nodes) {
        nodes.sort(Comparator.comparing(MenuTreeNodeVO::getSort, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(MenuTreeNodeVO::getId, Comparator.nullsLast(Long::compareTo)));
        for (MenuTreeNodeVO node : nodes) {
            if (!CollectionUtils.isEmpty(node.getChildren())) {
                sortNodes(node.getChildren());
            }
        }
    }

    private MenuTreeNodeVO toMenuNode(Menu menu) {
        MenuTreeNodeVO node = new MenuTreeNodeVO();
        node.setId(menu.getId());
        node.setParentId(menu.getParentId());
        node.setMenuName(menu.getMenuName());
        node.setMenuCode(menu.getMenuCode());
        node.setPath(menu.getPath());
        node.setComponent(menu.getComponent());
        node.setIcon(menu.getIcon());
        node.setSort(menu.getSort());
        node.setType(menu.getType());
        node.setMeta(parseMeta(menu.getMetaJson()));
        return node;
    }

    private MenuMetaVO parseMeta(String metaJson) {
        if (!StringUtils.hasText(metaJson)) {
            return new MenuMetaVO();
        }
        try {
            return objectMapper.readValue(metaJson, MenuMetaVO.class);
        } catch (Exception ex) {
            log.warn("Failed to parse menu meta: {}", metaJson, ex);
            return new MenuMetaVO();
        }
    }

    private RoleSummaryVO toRoleSummary(Role role) {
        RoleSummaryVO summary = new RoleSummaryVO();
        summary.setId(role.getId());
        summary.setRoleCode(role.getRoleCode());
        summary.setRoleName(role.getRoleName());
        return summary;
    }

    private RoleSummaryVO toRoleSummary(UserRoleViewDTO roleView) {
        RoleSummaryVO summary = new RoleSummaryVO();
        summary.setId(roleView.getRoleId());
        summary.setRoleCode(roleView.getRoleCode());
        summary.setRoleName(roleView.getRoleName());
        return summary;
    }

    private boolean isButton(Menu menu) {
        return Integer.valueOf(2).equals(menu.getType());
    }

    private String resolveHomePath(List<Role> roles, List<MenuTreeNodeVO> menus) {
        Set<String> roleCodes = roles.stream()
                .map(Role::getRoleCode)
                .map(this::normalizeCode)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> rootMenuCodes = menus.stream()
                .map(MenuTreeNodeVO::getMenuCode)
                .map(this::normalizeCode)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        for (RoleHomePreference preference : ROLE_HOME_PREFERENCES) {
            if (roleCodes.contains(normalizeCode(preference.roleCode()))
                    && rootMenuCodes.contains(normalizeCode(preference.menuCode()))) {
                return preference.landingPath();
            }
        }

        return resolveFirstMenuPath(menus);
    }

    private String resolveFirstMenuPath(List<MenuTreeNodeVO> menus) {
        for (MenuTreeNodeVO menu : menus) {
            String path = resolveMenuPath(menu);
            if (StringUtils.hasText(path)) {
                return path;
            }
        }
        return "/";
    }

    private String resolveMenuPath(MenuTreeNodeVO menu) {
        if (StringUtils.hasText(menu.getPath())) {
            return menu.getPath();
        }
        if (CollectionUtils.isEmpty(menu.getChildren())) {
            return null;
        }
        for (MenuTreeNodeVO child : menu.getChildren()) {
            String path = resolveMenuPath(child);
            if (StringUtils.hasText(path)) {
                return path;
            }
        }
        return null;
    }

    private Comparator<Menu> menuComparator() {
        return Comparator.comparing(Menu::getSort, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(Menu::getId, Comparator.nullsLast(Long::compareTo));
    }

    private String normalizeCode(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase() : "";
    }

    private record RoleHomePreference(String roleCode, String menuCode, String landingPath) {
    }
}
