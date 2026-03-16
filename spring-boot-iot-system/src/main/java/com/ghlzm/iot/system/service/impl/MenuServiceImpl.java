package com.ghlzm.iot.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.system.entity.Menu;
import com.ghlzm.iot.system.mapper.MenuMapper;
import com.ghlzm.iot.system.mapper.RoleMenuMapper;
import com.ghlzm.iot.system.service.MenuService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

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
        LambdaQueryWrapper<Menu> wrapper = new LambdaQueryWrapper<>();
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
        return list(wrapper);
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

