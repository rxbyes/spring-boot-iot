package com.ghlzm.iot.system.controller;

import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.framework.security.JwtUserPrincipal;
import com.ghlzm.iot.system.entity.Menu;
import com.ghlzm.iot.system.security.GovernancePermissionCodes;
import com.ghlzm.iot.system.security.GovernancePermissionGuard;
import com.ghlzm.iot.system.service.MenuService;
import com.ghlzm.iot.system.service.PermissionService;
import com.ghlzm.iot.system.vo.MenuTreeNodeVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
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
      private final GovernancePermissionGuard permissionGuard;

      public MenuController(PermissionService permissionService, MenuService menuService) {
            this(permissionService, menuService, null);
      }

      @Autowired
      public MenuController(PermissionService permissionService,
                            MenuService menuService,
                            GovernancePermissionGuard permissionGuard) {
            this.permissionService = permissionService;
            this.menuService = menuService;
            this.permissionGuard = permissionGuard;
      }

      @GetMapping("/tree")
      public R<List<MenuTreeNodeVO>> listMenuTree(Authentication authentication) {
            return R.ok(permissionService.listMenuTree(requireCurrentUserId(authentication)));
      }

      @GetMapping("/list")
      public R<List<Menu>> listMenus(@RequestParam(required = false) Long parentId,
                                     @RequestParam(required = false) String menuName,
                                     @RequestParam(required = false) String menuCode,
                                     @RequestParam(required = false) Integer type,
                                     @RequestParam(required = false) Integer status,
                                     Authentication authentication) {
            Long currentUserId = requireCurrentUserId(authentication);
            if (parentId != null) {
                  return R.ok(menuService.listMenusByParentId(currentUserId, parentId));
            }
            return R.ok(menuService.listMenus(currentUserId, menuName, menuCode, type, status));
      }

      @GetMapping("/page")
      public R<PageResult<Menu>> pageMenus(@RequestParam(required = false) String menuName,
                                           @RequestParam(required = false) String menuCode,
                                           @RequestParam(required = false) Integer type,
                                           @RequestParam(required = false) Integer status,
                                           @RequestParam(defaultValue = "1") Long pageNum,
                                           @RequestParam(defaultValue = "10") Long pageSize,
                                           Authentication authentication) {
            return R.ok(menuService.pageMenus(
                    requireCurrentUserId(authentication),
                    menuName,
                    menuCode,
                    type,
                    status,
                    pageNum,
                    pageSize
            ));
      }

      @GetMapping("/{id}")
      public R<Menu> getById(@PathVariable Long id, Authentication authentication) {
            return R.ok(menuService.getMenuById(requireCurrentUserId(authentication), id));
      }

      @PostMapping("/add")
      public R<Menu> addMenu(@RequestBody Menu menu, Authentication authentication) {
            Long currentUserId = requireCurrentUserId(authentication);
            requirePermission(currentUserId, "新增菜单", GovernancePermissionCodes.MENU_ADD);
            return R.ok(menuService.addMenu(currentUserId, menu));
      }

      @PutMapping("/update")
      public R<Void> updateMenu(@RequestBody Menu menu, Authentication authentication) {
            Long currentUserId = requireCurrentUserId(authentication);
            requirePermission(currentUserId, "编辑菜单", GovernancePermissionCodes.MENU_UPDATE);
            menuService.updateMenu(currentUserId, menu);
            return R.ok();
      }

      @DeleteMapping("/{id}")
      public R<Void> deleteMenu(@PathVariable Long id, Authentication authentication) {
            Long currentUserId = requireCurrentUserId(authentication);
            requirePermission(currentUserId, "删除菜单", GovernancePermissionCodes.MENU_DELETE);
            menuService.deleteMenu(currentUserId, id);
            return R.ok();
      }

      private Long requireCurrentUserId(Authentication authentication) {
            if (authentication == null || !(authentication.getPrincipal() instanceof JwtUserPrincipal principal)) {
                  throw new com.ghlzm.iot.common.exception.BizException(401, "未认证，请先登录");
            }
            return principal.userId();
      }

      private void requirePermission(Long currentUserId, String actionName, String permissionCode) {
            if (permissionGuard != null) {
                  permissionGuard.requireAnyPermission(currentUserId, actionName, permissionCode);
            }
      }
}
