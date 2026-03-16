package com.ghlzm.iot.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ghlzm.iot.system.entity.Menu;

import java.util.List;

public interface MenuService extends IService<Menu> {

    Menu addMenu(Menu menu);

    List<Menu> listMenus(String menuName, String menuCode, Integer type, Integer status);

    Menu getMenuById(Long id);

    void updateMenu(Menu menu);

    void deleteMenu(Long id);
}

