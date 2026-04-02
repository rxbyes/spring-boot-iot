package com.ghlzm.iot.system.controller;

import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.framework.security.JwtUserPrincipal;
import com.ghlzm.iot.system.entity.Organization;
import com.ghlzm.iot.system.service.OrganizationService;
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
@RequestMapping("/api/organization")
public class OrganizationController {

      private final OrganizationService organizationService;

      public OrganizationController(OrganizationService organizationService) {
            this.organizationService = organizationService;
      }

      @PostMapping
      public R<Organization> addOrganization(@RequestBody Organization organization) {
            Organization result = organizationService.addOrganization(organization);
            return R.ok(result);
      }

      @GetMapping("/list")
      public R<List<Organization>> listOrganizations(@RequestParam(required = false) Long parentId,
                                                     Authentication authentication) {
            return R.ok(organizationService.listOrganizations(requireCurrentUserId(authentication), parentId));
      }

      @GetMapping("/page")
      public R<PageResult<Organization>> pageOrganizations(@RequestParam(required = false) String orgName,
                                                           @RequestParam(required = false) String orgCode,
                                                           @RequestParam(required = false) Integer status,
                                                           @RequestParam(defaultValue = "1") Long pageNum,
                                                           @RequestParam(defaultValue = "10") Long pageSize,
                                                           Authentication authentication) {
            return R.ok(organizationService.pageOrganizations(requireCurrentUserId(authentication), orgName, orgCode, status, pageNum, pageSize));
      }

      @GetMapping("/tree")
      public R<List<Organization>> listOrganizationTree(Authentication authentication) {
            return R.ok(organizationService.listOrganizationTree(requireCurrentUserId(authentication)));
      }

      @GetMapping("/{id}")
      public R<Organization> getById(@PathVariable Long id, Authentication authentication) {
            return R.ok(organizationService.getById(requireCurrentUserId(authentication), id));
      }

      @PutMapping
      public R<Void> updateOrganization(@RequestBody Organization organization) {
            organizationService.updateOrganization(organization);
            return R.ok();
      }

      @DeleteMapping("/{id}")
      public R<Void> deleteOrganization(@PathVariable Long id) {
            organizationService.deleteOrganization(id);
            return R.ok();
      }

      private Long requireCurrentUserId(Authentication authentication) {
            if (authentication == null || !(authentication.getPrincipal() instanceof JwtUserPrincipal principal)) {
                  throw new com.ghlzm.iot.common.exception.BizException(401, "未认证，请先登录");
            }
            return principal.userId();
      }
}
