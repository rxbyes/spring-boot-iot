package com.ghlzm.iot.system.controller;

import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.system.service.PermissionService;
import com.ghlzm.iot.system.vo.MenuTreeNodeVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/menu")
public class MenuController {

    private final PermissionService permissionService;

    public MenuController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @GetMapping("/tree")
    public R<List<MenuTreeNodeVO>> listMenuTree() {
        return R.ok(permissionService.listMenuTree());
    }
}
