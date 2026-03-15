package com.ghlzm.iot.system.controller;

import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.system.entity.Region;
import com.ghlzm.iot.system.service.RegionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 区域管理 Controller
 */
@RestController
@RequestMapping("/api/region")
public class RegionController {

      @Autowired
      private RegionService regionService;

      /**
       * 查询区域列表
       */
      @GetMapping("/list")
      public R<List<Region>> listRegions(@RequestParam(required = false) Long parentId) {
            List<Region> regions = regionService.listRegions(parentId);
            return R.ok(regions);
      }

      /**
       * 查询区域树
       */
      @GetMapping("/tree")
      public R<List<Region>> listRegionTree() {
            List<Region> regions = regionService.listRegionTree();
            return R.ok(regions);
      }

      /**
       * 根据ID查询区域
       */
      @GetMapping("/{id}")
      public R<Region> getById(@PathVariable Long id) {
            Region region = regionService.getById(id);
            return R.ok(region);
      }

      /**
       * 添加区域
       */
      @PostMapping
      public R<Region> addRegion(@RequestBody Region region) {
            regionService.addRegion(region);
            return R.ok(region);
      }

      /**
       * 更新区域
       */
      @PutMapping
      public R<Region> updateRegion(@RequestBody Region region) {
            regionService.updateRegion(region);
            return R.ok(region);
      }

      /**
       * 删除区域
       */
      @DeleteMapping("/{id}")
      public R<Void> deleteRegion(@PathVariable Long id) {
            regionService.deleteRegion(id);
            return R.ok();
      }
}
