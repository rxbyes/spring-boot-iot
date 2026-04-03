package com.ghlzm.iot.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.system.entity.Menu;

import java.util.List;

public interface MenuService extends IService<Menu> {

      Menu addMenu(Menu menu);

      Menu addMenu(Long currentUserId, Menu menu);

      List<Menu> listMenus(String menuName, String menuCode, Integer type, Integer status);

      List<Menu> listMenus(Long currentUserId, String menuName, String menuCode, Integer type, Integer status);

      List<Menu> listMenusByParentId(Long parentId);

      List<Menu> listMenusByParentId(Long currentUserId, Long parentId);

      PageResult<Menu> pageMenus(String menuName, String menuCode, Integer type, Integer status, Long pageNum, Long pageSize);

      PageResult<Menu> pageMenus(Long currentUserId,
                                 String menuName,
                                 String menuCode,
                                 Integer type,
                                 Integer status,
                                 Long pageNum,
                                 Long pageSize);

      Menu getMenuById(Long id);

      Menu getMenuById(Long currentUserId, Long id);

      void updateMenu(Menu menu);

      void updateMenu(Long currentUserId, Menu menu);

      void deleteMenu(Long id);

      void deleteMenu(Long currentUserId, Long id);
}
