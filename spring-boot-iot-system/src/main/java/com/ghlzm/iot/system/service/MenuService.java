package com.ghlzm.iot.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.system.entity.Menu;

import java.util.List;

public interface MenuService extends IService<Menu> {

      Menu addMenu(Menu menu);

      List<Menu> listMenus(String menuName, String menuCode, Integer type, Integer status);

      List<Menu> listMenusByParentId(Long parentId);

      PageResult<Menu> pageMenus(String menuName, String menuCode, Integer type, Integer status, Long pageNum, Long pageSize);

      Menu getMenuById(Long id);

      void updateMenu(Menu menu);

      void deleteMenu(Long id);
}
