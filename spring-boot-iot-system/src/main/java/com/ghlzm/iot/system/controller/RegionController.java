package com.ghlzm.iot.system.controller;

import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.system.entity.Region;
import com.ghlzm.iot.system.service.RegionService;
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

      public RegionController(RegionService regionService) {
            this.regionService = regionService;
      }

      @GetMapping("/list")
      public R<List<Region>> listRegions(@RequestParam(required = false) Long parentId) {
            return R.ok(regionService.listRegions(parentId));
      }

      @GetMapping("/page")
      public R<PageResult<Region>> pageRegions(@RequestParam(required = false) String regionName,
                                               @RequestParam(required = false) String regionCode,
                                               @RequestParam(required = false) String regionType,
                                               @RequestParam(defaultValue = "1") Long pageNum,
                                               @RequestParam(defaultValue = "10") Long pageSize) {
            return R.ok(regionService.pageRegions(regionName, regionCode, regionType, pageNum, pageSize));
      }

      @GetMapping("/tree")
      public R<List<Region>> listRegionTree() {
            return R.ok(regionService.listRegionTree());
      }

      @GetMapping("/{id}")
      public R<Region> getById(@PathVariable Long id) {
            return R.ok(regionService.getById(id));
      }

      @PostMapping
      public R<Region> addRegion(@RequestBody Region region) {
            regionService.addRegion(region);
            return R.ok(region);
      }

      @PutMapping
      public R<Region> updateRegion(@RequestBody Region region) {
            regionService.updateRegion(region);
            return R.ok(region);
      }

      @DeleteMapping("/{id}")
      public R<Void> deleteRegion(@PathVariable Long id) {
            regionService.deleteRegion(id);
            return R.ok();
      }
}
