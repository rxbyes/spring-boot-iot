package com.ghlzm.iot.system.controller;

import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.system.entity.Menu;
import com.ghlzm.iot.system.service.MenuService;
import com.ghlzm.iot.system.service.PermissionService;
import com.ghlzm.iot.system.vo.MenuTreeNodeVO;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/menu")
public class MenuController {

    private final PermissionService permissionService;
    private final MenuService menuService;

    public MenuController(PermissionService permissionService, MenuService menuService) {
        this.permissionService = permissionService;
        this.menuService = menuService;
    }

    @GetMapping("/tree")
    public R<List<MenuTreeNodeVO>> listMenuTree() {
        return R.ok(permissionService.listMenuTree());
    }

    @GetMapping("/list")
    public R<List<Menu>> listMenus(@RequestParam(required = false) String menuName,
                                   @RequestParam(required = false) String menuCode,
                                   @RequestParam(required = false) Integer type,
                                   @RequestParam(required = false) Integer status) {
        return R.ok(menuService.listMenus(menuName, menuCode, type, status));
    }

    @GetMapping("/{id}")
    public R<Menu> getById(@PathVariable Long id) {
        return R.ok(menuService.getMenuById(id));
    }

    @PostMapping("/add")
    public R<Menu> addMenu(@RequestBody Menu menu) {
        return R.ok(menuService.addMenu(menu));
    }

    @PutMapping("/update")
    public R<Void> updateMenu(@RequestBody Menu menu) {
        menuService.updateMenu(menu);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> deleteMenu(@PathVariable Long id) {
        menuService.deleteMenu(id);
        return R.ok();
    }
}
