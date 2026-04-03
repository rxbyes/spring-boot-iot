package com.ghlzm.iot.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.framework.mybatis.PageQueryUtils;
import com.ghlzm.iot.system.entity.Menu;
import com.ghlzm.iot.system.mapper.MenuMapper;
import com.ghlzm.iot.system.mapper.RoleMenuMapper;
import com.ghlzm.iot.system.service.MenuService;
import com.ghlzm.iot.system.service.PermissionService;
import com.ghlzm.iot.system.service.model.DataPermissionContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MenuServiceImpl extends ServiceImpl<MenuMapper, Menu> implements MenuService {

      private static final String TABLE_NAME = "sys_menu";

      private final RoleMenuMapper roleMenuMapper;
      private final JdbcTemplate jdbcTemplate;
      private final MenuSchemaSupport menuSchemaSupport;
      private final PermissionService permissionService;

      public MenuServiceImpl(RoleMenuMapper roleMenuMapper,
                             JdbcTemplate jdbcTemplate,
                             MenuSchemaSupport menuSchemaSupport,
                             PermissionService permissionService) {
            this.roleMenuMapper = roleMenuMapper;
            this.jdbcTemplate = jdbcTemplate;
            this.menuSchemaSupport = menuSchemaSupport;
            this.permissionService = permissionService;
      }

      @Override
      @Transactional(rollbackFor = Exception.class)
      public Menu addMenu(Menu menu) {
            return addMenu(null, menu);
      }

      @Override
      @Transactional(rollbackFor = Exception.class)
      public Menu addMenu(Long currentUserId, Menu menu) {
            Long tenantId = resolveTenantId(currentUserId, menu.getTenantId());
            menu.setTenantId(normalizeTenantId(tenantId));

            validateMenuCodeUnique(menu.getTenantId(), menu.getMenuCode(), null);
            validateParent(currentUserId, menu.getTenantId(), menu.getParentId(), null);

            normalizeForInsert(currentUserId, menu);
            if (!insertMenuCompat(menu)) {
                  save(menu);
            }
            return getMenuById(currentUserId, menu.getId());
      }

      @Override
      public List<Menu> listMenus(String menuName, String menuCode, Integer type, Integer status) {
            return listMenus(null, menuName, menuCode, type, status);
      }

      @Override
      public List<Menu> listMenus(Long currentUserId, String menuName, String menuCode, Integer type, Integer status) {
            List<Menu> menus = list(buildMenuQueryWrapper(currentUserId, menuName, menuCode, type, status, true));
            fillHasChildren(menus);
            return menus;
      }

      @Override
      public List<Menu> listMenusByParentId(Long parentId) {
            return listMenusByParentId(null, parentId);
      }

      @Override
      public List<Menu> listMenusByParentId(Long currentUserId, Long parentId) {
            Long tenantId = resolveTenantId(currentUserId, null);
            LambdaQueryWrapper<Menu> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(tenantId != null, Menu::getTenantId, tenantId)
                    .eq(Menu::getParentId, parentId != null && parentId > 0 ? parentId : 0L)
                    .orderByAsc(Menu::getSort)
                    .orderByAsc(Menu::getId);
            List<Menu> menus = list(wrapper);
            fillHasChildren(menus);
            return menus;
      }

      @Override
      public PageResult<Menu> pageMenus(String menuName, String menuCode, Integer type, Integer status, Long pageNum, Long pageSize) {
            return pageMenus(null, menuName, menuCode, type, status, pageNum, pageSize);
      }

      @Override
      public PageResult<Menu> pageMenus(Long currentUserId,
                                        String menuName,
                                        String menuCode,
                                        Integer type,
                                        Integer status,
                                        Long pageNum,
                                        Long pageSize) {
            Page<Menu> page = PageQueryUtils.buildPage(pageNum, pageSize);
            Page<Menu> result = page(page, buildMenuQueryWrapper(currentUserId, menuName, menuCode, type, status, false));
            List<Menu> records = result.getRecords();
            fillHasChildren(records);
            return PageQueryUtils.toPageResult(result);
      }

      @Override
      public Menu getMenuById(Long id) {
            return getMenuById(null, id);
      }

      @Override
      public Menu getMenuById(Long currentUserId, Long id) {
            Menu menu = super.getById(id);
            if (menu == null) {
                  return null;
            }
            ensureMenuAccessible(currentUserId, menu);
            return menu;
      }

      @Override
      @Transactional(rollbackFor = Exception.class)
      public void updateMenu(Menu menu) {
            updateMenu(null, menu);
      }

      @Override
      @Transactional(rollbackFor = Exception.class)
      public void updateMenu(Long currentUserId, Menu menu) {
            if (menu.getId() == null) {
                  throw new BizException("菜单ID不能为空");
            }
            Menu existing = super.getById(menu.getId());
            if (existing == null) {
                  throw new BizException("菜单不存在");
            }
            ensureMenuAccessible(currentUserId, existing);

            validateMenuCodeUnique(existing.getTenantId(),
                    StringUtils.hasText(menu.getMenuCode()) ? menu.getMenuCode() : existing.getMenuCode(),
                    menu.getId());
            validateParent(currentUserId,
                    existing.getTenantId(),
                    menu.getParentId() == null ? existing.getParentId() : menu.getParentId(),
                    menu.getId());

            normalizeForUpdate(currentUserId, menu, existing);
            if (!updateMenuCompat(menu)) {
                  updateById(menu);
            }
      }

      @Override
      @Transactional(rollbackFor = Exception.class)
      public void deleteMenu(Long id) {
            deleteMenu(null, id);
      }

      @Override
      @Transactional(rollbackFor = Exception.class)
      public void deleteMenu(Long currentUserId, Long id) {
            Menu existing = super.getById(id);
            if (existing == null) {
                  throw new BizException("菜单不存在");
            }
            ensureMenuAccessible(currentUserId, existing);

            LambdaQueryWrapper<Menu> childWrapper = new LambdaQueryWrapper<>();
            childWrapper.eq(Menu::getParentId, id)
                    .eq(existing.getTenantId() != null, Menu::getTenantId, existing.getTenantId());
            if (count(childWrapper) > 0) {
                  throw new BizException("存在子菜单，无法删除");
            }

            if (roleMenuMapper.countByMenuId(id) > 0) {
                  throw new BizException("菜单已被角色引用，无法删除");
            }

            removeById(id);
      }

      private LambdaQueryWrapper<Menu> buildMenuQueryWrapper(Long currentUserId,
                                                             String menuName,
                                                             String menuCode,
                                                             Integer type,
                                                             Integer status,
                                                             boolean listMode) {
            boolean filterMode = StringUtils.hasText(menuName)
                    || StringUtils.hasText(menuCode)
                    || type != null
                    || status != null;
            Long tenantId = resolveTenantId(currentUserId, null);
            LambdaQueryWrapper<Menu> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(tenantId != null, Menu::getTenantId, tenantId);
            if (!listMode && !filterMode) {
                  wrapper.eq(Menu::getParentId, 0L);
            }
            if (StringUtils.hasText(menuName)) {
                  wrapper.like(Menu::getMenuName, menuName.trim());
            }
            if (StringUtils.hasText(menuCode)) {
                  wrapper.like(Menu::getMenuCode, menuCode.trim());
            }
            if (type != null) {
                  wrapper.eq(Menu::getType, type);
            }
            if (status != null) {
                  wrapper.eq(Menu::getStatus, status);
            }
            wrapper.orderByAsc(Menu::getSort).orderByAsc(Menu::getId);
            return wrapper;
      }

      private void fillHasChildren(List<Menu> menus) {
            if (CollectionUtils.isEmpty(menus)) {
                  return;
            }
            Set<Long> ids = menus.stream()
                    .map(Menu::getId)
                    .filter(id -> id != null && id > 0)
                    .collect(Collectors.toSet());
            if (ids.isEmpty()) {
                  menus.forEach(item -> item.setHasChildren(Boolean.FALSE));
                  return;
            }

            Set<Long> tenantIds = menus.stream()
                    .map(Menu::getTenantId)
                    .filter(id -> id != null && id > 0)
                    .collect(Collectors.toSet());
            List<Menu> children = list(new LambdaQueryWrapper<Menu>()
                    .select(Menu::getParentId)
                    .in(Menu::getParentId, ids)
                    .in(!tenantIds.isEmpty(), Menu::getTenantId, tenantIds));
            Set<Long> parentIds = children.stream()
                    .map(Menu::getParentId)
                    .filter(parentId -> parentId != null && parentId > 0)
                    .collect(Collectors.toSet());
            menus.forEach(item -> item.setHasChildren(parentIds.contains(item.getId())));
      }

      private void validateMenuCodeUnique(Long tenantId, String menuCode, Long currentId) {
            if (!StringUtils.hasText(menuCode)) {
                  return;
            }
            LambdaQueryWrapper<Menu> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(tenantId != null, Menu::getTenantId, tenantId)
                    .eq(Menu::getMenuCode, menuCode.trim());
            if (currentId != null) {
                  wrapper.ne(Menu::getId, currentId);
            }
            if (count(wrapper) > 0) {
                  throw new BizException("菜单编码已存在");
            }
      }

      private void validateParent(Long currentUserId, Long tenantId, Long parentId, Long currentId) {
            if (parentId == null || parentId <= 0) {
                  return;
            }
            if (currentId != null && parentId.equals(currentId)) {
                  throw new BizException("父级菜单不能是自身");
            }
            Menu parent = super.getById(parentId);
            if (parent == null) {
                  throw new BizException("父级菜单不存在");
            }
            if (tenantId != null && parent.getTenantId() != null && !tenantId.equals(parent.getTenantId())) {
                  throw new BizException("父级菜单不存在或无权访问");
            }
            ensureMenuAccessible(currentUserId, parent);
      }

      private void normalizeForInsert(Long currentUserId, Menu menu) {
            if (menu.getId() == null) {
                  menu.setId(IdWorker.getId());
            }
            if (menu.getTenantId() == null) {
                  menu.setTenantId(1L);
            }
            if (menu.getParentId() == null) {
                  menu.setParentId(0L);
            }
            if (menu.getSort() == null) {
                  menu.setSort(0);
            }
            if (menu.getType() == null) {
                  menu.setType(1);
            }
            if (menu.getStatus() == null) {
                  menu.setStatus(1);
            }
            if (menu.getCreateBy() == null) {
                  menu.setCreateBy(currentUserId == null ? 1L : currentUserId);
            }
            if (menu.getCreateTime() == null) {
                  menu.setCreateTime(new Date());
            }
            if (menu.getUpdateBy() == null) {
                  menu.setUpdateBy(menu.getCreateBy());
            }
            if (menu.getUpdateTime() == null) {
                  menu.setUpdateTime(menu.getCreateTime());
            }
            if (menu.getDeleted() == null) {
                  menu.setDeleted(0);
            }
      }

      private void normalizeForUpdate(Long currentUserId, Menu menu, Menu existing) {
            menu.setTenantId(existing.getTenantId());
            if (menu.getParentId() == null) {
                  menu.setParentId(existing.getParentId());
            }
            if (!StringUtils.hasText(menu.getMenuName())) {
                  menu.setMenuName(existing.getMenuName());
            }
            if (!StringUtils.hasText(menu.getMenuCode())) {
                  menu.setMenuCode(existing.getMenuCode());
            }
            if (menu.getPath() == null) {
                  menu.setPath(existing.getPath());
            }
            if (menu.getComponent() == null) {
                  menu.setComponent(existing.getComponent());
            }
            if (menu.getIcon() == null) {
                  menu.setIcon(existing.getIcon());
            }
            if (menu.getMetaJson() == null) {
                  menu.setMetaJson(existing.getMetaJson());
            }
            if (menu.getSort() == null) {
                  menu.setSort(existing.getSort());
            }
            if (menu.getType() == null) {
                  menu.setType(existing.getType());
            }
            if (menu.getStatus() == null) {
                  menu.setStatus(existing.getStatus());
            }
            if (menu.getUpdateBy() == null) {
                  menu.setUpdateBy(currentUserId == null
                          ? (existing.getUpdateBy() == null ? existing.getCreateBy() : existing.getUpdateBy())
                          : currentUserId);
            }
            if (menu.getUpdateTime() == null) {
                  menu.setUpdateTime(new Date());
            }
      }

      private boolean insertMenuCompat(Menu menu) {
            Set<String> columns = menuSchemaSupport.getColumns();
            if (columns.isEmpty()) {
                  return false;
            }

            Map<String, Object> values = new LinkedHashMap<>();
            putValue(values, columns, "id", menu.getId());
            putValue(values, columns, "tenant_id", menu.getTenantId());
            putValue(values, columns, "parent_id", menu.getParentId());
            putValue(values, columns, "menu_name", menu.getMenuName());
            putAliasedValue(values, columns, menu.getMenuCode(), "menu_code", "permission");
            putAliasedValue(values, columns, menu.getPath(), "path", "route_path");
            putValue(values, columns, "component", menu.getComponent());
            putValue(values, columns, "icon", menu.getIcon());
            putValue(values, columns, "meta_json", menu.getMetaJson());
            putAliasedValue(values, columns, menu.getSort(), "sort", "sort_no");
            putValue(values, columns, "type", menu.getType());
            putValue(values, columns, "menu_type", menu.getType());
            putValue(values, columns, "visible", 1);
            putValue(values, columns, "status", menu.getStatus());
            putValue(values, columns, "create_by", menu.getCreateBy());
            putValue(values, columns, "create_time", menu.getCreateTime());
            putValue(values, columns, "update_by", menu.getUpdateBy());
            putValue(values, columns, "update_time", menu.getUpdateTime());
            putValue(values, columns, "deleted", menu.getDeleted());
            if (values.isEmpty()) {
                  return false;
            }

            // 兼容真实库仍保留 menu_type / permission / route_path / sort_no 的场景，插入时显式带上同义列。
            String sql = "INSERT INTO " + TABLE_NAME + " (" + String.join(", ", values.keySet()) + ") VALUES ("
                    + String.join(", ", values.keySet().stream().map(item -> "?").toList()) + ")";
            jdbcTemplate.update(sql, values.values().toArray());
            return true;
      }

      private boolean updateMenuCompat(Menu menu) {
            Set<String> columns = menuSchemaSupport.getColumns();
            if (columns.isEmpty()) {
                  return false;
            }

            Map<String, Object> values = new LinkedHashMap<>();
            putValue(values, columns, "parent_id", menu.getParentId());
            putValue(values, columns, "menu_name", menu.getMenuName());
            putAliasedValue(values, columns, menu.getMenuCode(), "menu_code", "permission");
            putAliasedValue(values, columns, menu.getPath(), "path", "route_path");
            putValue(values, columns, "component", menu.getComponent());
            putValue(values, columns, "icon", menu.getIcon());
            putValue(values, columns, "meta_json", menu.getMetaJson());
            putAliasedValue(values, columns, menu.getSort(), "sort", "sort_no");
            putValue(values, columns, "type", menu.getType());
            putValue(values, columns, "menu_type", menu.getType());
            putValue(values, columns, "visible", 1);
            putValue(values, columns, "status", menu.getStatus());
            putValue(values, columns, "update_by", menu.getUpdateBy());
            putValue(values, columns, "update_time", menu.getUpdateTime());
            if (values.isEmpty()) {
                  return false;
            }

            StringBuilder sql = new StringBuilder("UPDATE ").append(TABLE_NAME).append(" SET ");
            List<Object> params = new ArrayList<>();
            boolean first = true;
            for (Map.Entry<String, Object> entry : values.entrySet()) {
                  if (!first) {
                        sql.append(", ");
                  }
                  sql.append(entry.getKey()).append(" = ?");
                  params.add(entry.getValue());
                  first = false;
            }
            sql.append(" WHERE id = ?");
            params.add(menu.getId());
            if (columns.contains("tenant_id") && menu.getTenantId() != null) {
                  sql.append(" AND tenant_id = ?");
                  params.add(menu.getTenantId());
            }
            jdbcTemplate.update(sql.toString(), params.toArray());
            return true;
      }

      private void putValue(Map<String, Object> values, Set<String> columns, String column, Object value) {
            if (value != null && columns.contains(column)) {
                  values.put(column, toJdbcValue(value));
            }
      }

      private void putAliasedValue(Map<String, Object> values, Set<String> columns, Object value, String... candidates) {
            if (value == null) {
                  return;
            }
            for (String candidate : candidates) {
                  if (columns.contains(candidate)) {
                        values.put(candidate, toJdbcValue(value));
                  }
            }
      }

      private Object toJdbcValue(Object value) {
            if (value instanceof Date date) {
                  return new Timestamp(date.getTime());
            }
            return value;
      }

      private Long resolveTenantId(Long currentUserId, Long fallbackTenantId) {
            if (currentUserId == null) {
                  return fallbackTenantId;
            }
            return permissionService.getDataPermissionContext(currentUserId).tenantId();
      }

      private Long normalizeTenantId(Long tenantId) {
            return tenantId == null ? 1L : tenantId;
      }

      private void ensureMenuAccessible(Long currentUserId, Menu menu) {
            if (currentUserId == null || menu == null) {
                  return;
            }
            DataPermissionContext context = permissionService.getDataPermissionContext(currentUserId);
            if (context.superAdmin()) {
                  return;
            }
            if (context.tenantId() != null && !context.tenantId().equals(menu.getTenantId())) {
                  throw new BizException("菜单不存在或无权访问");
            }
      }
}
