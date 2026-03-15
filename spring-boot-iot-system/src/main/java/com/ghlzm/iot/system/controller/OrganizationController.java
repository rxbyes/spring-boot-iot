package com.ghlzm.iot.system.controller;

import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.system.entity.Organization;
import com.ghlzm.iot.system.service.OrganizationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 组织机构 Controller
 */
@RestController
@RequestMapping("/api/organization")
public class OrganizationController {

      private final OrganizationService organizationService;

      public OrganizationController(OrganizationService organizationService) {
            this.organizationService = organizationService;
      }

      /**
       * 添加组织机构
       */
      @PostMapping
      public R<Organization> addOrganization(@RequestBody Organization organization) {
            Organization result = organizationService.addOrganization(organization);
            return R.ok(result);
      }

      /**
       * 查询组织机构列表
       */
      @GetMapping("/list")
      public R<List<Organization>> listOrganizations(@RequestParam(required = false) Long parentId) {
            List<Organization> list = organizationService.listOrganizations(parentId);
            return R.ok(list);
      }

      /**
       * 查询组织机构树
       */
      @GetMapping("/tree")
      public R<List<Organization>> listOrganizationTree() {
            List<Organization> tree = organizationService.listOrganizationTree();
            return R.ok(tree);
      }

      /**
       * 根据ID查询组织机构
       */
      @GetMapping("/{id}")
      public R<Organization> getById(@PathVariable Long id) {
            Organization organization = organizationService.getById(id);
            return R.ok(organization);
      }

      /**
       * 更新组织机构
       */
      @PutMapping
      public R<Void> updateOrganization(@RequestBody Organization organization) {
            organizationService.updateOrganization(organization);
            return R.ok();
      }

      /**
       * 删除组织机构
       */
      @DeleteMapping("/{id}")
      public R<Void> deleteOrganization(@PathVariable Long id) {
            organizationService.deleteOrganization(id);
            return R.ok();
      }
}
