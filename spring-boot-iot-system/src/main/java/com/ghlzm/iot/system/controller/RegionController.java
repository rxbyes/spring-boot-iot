package com.ghlzm.iot.system.controller;

import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.framework.security.JwtUserPrincipal;
import com.ghlzm.iot.system.entity.Region;
import com.ghlzm.iot.system.security.GovernancePermissionCodes;
import com.ghlzm.iot.system.security.GovernancePermissionGuard;
import com.ghlzm.iot.system.service.RegionService;
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
@RequestMapping("/api/region")
public class RegionController {

      private final RegionService regionService;
      private final GovernancePermissionGuard permissionGuard;

      public RegionController(RegionService regionService) {
            this(regionService, null);
      }

      @Autowired
      public RegionController(RegionService regionService, GovernancePermissionGuard permissionGuard) {
            this.regionService = regionService;
            this.permissionGuard = permissionGuard;
      }

      @GetMapping("/list")
      public R<List<Region>> listRegions(@RequestParam(required = false) Long parentId,
                                         Authentication authentication) {
            return R.ok(regionService.listRegions(requireCurrentUserId(authentication), parentId));
      }

      @GetMapping("/page")
      public R<PageResult<Region>> pageRegions(@RequestParam(required = false) String regionName,
                                               @RequestParam(required = false) String regionCode,
                                               @RequestParam(required = false) String regionType,
                                               @RequestParam(defaultValue = "1") Long pageNum,
                                               @RequestParam(defaultValue = "10") Long pageSize,
                                               Authentication authentication) {
            return R.ok(regionService.pageRegions(requireCurrentUserId(authentication), regionName, regionCode, regionType, pageNum, pageSize));
      }

      @GetMapping("/tree")
      public R<List<Region>> listRegionTree(Authentication authentication) {
            return R.ok(regionService.listRegionTree(requireCurrentUserId(authentication)));
      }

      @GetMapping("/{id}")
      public R<Region> getById(@PathVariable Long id, Authentication authentication) {
            return R.ok(regionService.getById(requireCurrentUserId(authentication), id));
      }

      @PostMapping
      public R<Region> addRegion(@RequestBody Region region, Authentication authentication) {
            Long currentUserId = requireCurrentUserId(authentication);
            requirePermission(currentUserId, "新增区域", GovernancePermissionCodes.REGION_ADD);
            regionService.addRegion(currentUserId, region);
            return R.ok(region);
      }

      @PutMapping
      public R<Region> updateRegion(@RequestBody Region region, Authentication authentication) {
            Long currentUserId = requireCurrentUserId(authentication);
            requirePermission(currentUserId, "编辑区域", GovernancePermissionCodes.REGION_UPDATE);
            regionService.updateRegion(currentUserId, region);
            return R.ok(region);
      }

      @DeleteMapping("/{id}")
      public R<Void> deleteRegion(@PathVariable Long id, Authentication authentication) {
            Long currentUserId = requireCurrentUserId(authentication);
            requirePermission(currentUserId, "删除区域", GovernancePermissionCodes.REGION_DELETE);
            regionService.deleteRegion(currentUserId, id);
            return R.ok();
      }

      private void requirePermission(Long currentUserId, String actionName, String permissionCode) {
            if (permissionGuard != null) {
                  permissionGuard.requireAnyPermission(currentUserId, actionName, permissionCode);
            }
      }

      private Long requireCurrentUserId(Authentication authentication) {
            if (authentication == null || !(authentication.getPrincipal() instanceof JwtUserPrincipal principal)) {
                  throw new com.ghlzm.iot.common.exception.BizException(401, "未认证，请先登录");
            }
            return principal.userId();
      }
}
