package com.ghlzm.iot.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.framework.mybatis.PageQueryUtils;
import com.ghlzm.iot.system.entity.Menu;
import com.ghlzm.iot.system.mapper.MenuMapper;
import com.ghlzm.iot.system.mapper.RoleMenuMapper;
import com.ghlzm.iot.system.service.MenuService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MenuServiceImpl extends ServiceImpl<MenuMapper, Menu> implements MenuService {

      private final RoleMenuMapper roleMenuMapper;

      public MenuServiceImpl(RoleMenuMapper roleMenuMapper) {
            this.roleMenuMapper = roleMenuMapper;
      }

      @Override
      @Transactional(rollbackFor = Exception.class)
      public Menu addMenu(Menu menu) {
            validateMenuCodeUnique(menu.getMenuCode(), null);
            validateParent(menu.getParentId(), null);

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
                  menu.setCreateBy(1L);
            }

            save(menu);
            return getById(menu.getId());
      }

      @Override
      public List<Menu> listMenus(String menuName, String menuCode, Integer type, Integer status) {
            List<Menu> menus = list(buildMenuQueryWrapper(menuName, menuCode, type, status, true));
            fillHasChildren(menus);
            return menus;
      }

      @Override
      public List<Menu> listMenusByParentId(Long parentId) {
            LambdaQueryWrapper<Menu> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Menu::getParentId, parentId != null && parentId > 0 ? parentId : 0L)
                    .orderByAsc(Menu::getSort)
                    .orderByAsc(Menu::getId);
            List<Menu> menus = list(wrapper);
            fillHasChildren(menus);
            return menus;
      }

      @Override
      public PageResult<Menu> pageMenus(String menuName, String menuCode, Integer type, Integer status, Long pageNum, Long pageSize) {
            Page<Menu> page = PageQueryUtils.buildPage(pageNum, pageSize);
            Page<Menu> result = page(page, buildMenuQueryWrapper(menuName, menuCode, type, status, false));
            List<Menu> records = result.getRecords();
            fillHasChildren(records);
            return PageQueryUtils.toPageResult(result);
      }

      @Override
      public Menu getMenuById(Long id) {
            return getById(id);
      }

      @Override
      @Transactional(rollbackFor = Exception.class)
      public void updateMenu(Menu menu) {
            if (menu.getId() == null) {
                  throw new BizException("菜单ID不能为空");
            }
            Menu existing = getById(menu.getId());
            if (existing == null) {
                  throw new BizException("菜单不存在");
            }

            validateMenuCodeUnique(menu.getMenuCode(), menu.getId());
            validateParent(menu.getParentId(), menu.getId());

            if (menu.getParentId() == null) {
                  menu.setParentId(existing.getParentId());
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
                  menu.setUpdateBy(1L);
            }
            updateById(menu);
      }

      @Override
      @Transactional(rollbackFor = Exception.class)
      public void deleteMenu(Long id) {
            Menu existing = getById(id);
            if (existing == null) {
                  throw new BizException("菜单不存在");
            }

            LambdaQueryWrapper<Menu> childWrapper = new LambdaQueryWrapper<>();
            childWrapper.eq(Menu::getParentId, id);
            if (count(childWrapper) > 0) {
                  throw new BizException("存在子菜单，无法删除");
            }

            if (roleMenuMapper.countByMenuId(id) > 0) {
                  throw new BizException("菜单已被角色引用，无法删除");
            }

            removeById(id);
      }

      private LambdaQueryWrapper<Menu> buildMenuQueryWrapper(String menuName,
                                                             String menuCode,
                                                             Integer type,
                                                             Integer status,
                                                             boolean listMode) {
            boolean filterMode = StringUtils.hasText(menuName)
                    || StringUtils.hasText(menuCode)
                    || type != null
                    || status != null;
            LambdaQueryWrapper<Menu> wrapper = new LambdaQueryWrapper<>();
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

            List<Menu> children = list(new LambdaQueryWrapper<Menu>()
                    .select(Menu::getParentId)
                    .in(Menu::getParentId, ids));
            Set<Long> parentIds = children.stream()
                    .map(Menu::getParentId)
                    .filter(parentId -> parentId != null && parentId > 0)
                    .collect(Collectors.toSet());
            menus.forEach(item -> item.setHasChildren(parentIds.contains(item.getId())));
      }

      private void validateMenuCodeUnique(String menuCode, Long currentId) {
            if (!StringUtils.hasText(menuCode)) {
                  return;
            }
            LambdaQueryWrapper<Menu> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Menu::getMenuCode, menuCode.trim());
            if (currentId != null) {
                  wrapper.ne(Menu::getId, currentId);
            }
            if (count(wrapper) > 0) {
                  throw new BizException("菜单编码已存在");
            }
      }

      private void validateParent(Long parentId, Long currentId) {
            if (parentId == null || parentId <= 0) {
                  return;
            }
            if (currentId != null && parentId.equals(currentId)) {
                  throw new BizException("父级菜单不能是自身");
            }
            Menu parent = getById(parentId);
            if (parent == null) {
                  throw new BizException("父级菜单不存在");
            }
      }
}
