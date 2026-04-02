package com.ghlzm.iot.system.controller;

import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.system.entity.Dict;
import com.ghlzm.iot.system.entity.DictItem;
import com.ghlzm.iot.system.service.DictService;
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
@RequestMapping("/api/dict")
public class DictController {

      private final DictService dictService;

      public DictController(DictService dictService) {
            this.dictService = dictService;
      }

      @GetMapping("/list")
      public R<List<Dict>> listDicts(@RequestParam(required = false) String dictName,
                                     @RequestParam(required = false) String dictCode,
                                     @RequestParam(required = false) String dictType) {
            return R.ok(dictService.listDicts(dictName, dictCode, dictType));
      }

      @GetMapping("/page")
      public R<PageResult<Dict>> pageDicts(@RequestParam(required = false) String dictName,
                                           @RequestParam(required = false) String dictCode,
                                           @RequestParam(required = false) String dictType,
                                           @RequestParam(defaultValue = "1") Long pageNum,
                                           @RequestParam(defaultValue = "10") Long pageSize) {
            return R.ok(dictService.pageDicts(dictName, dictCode, dictType, pageNum, pageSize));
      }

      @GetMapping("/tree")
      public R<List<Dict>> listDictTree() {
            return R.ok(dictService.listDictTree());
      }

      @GetMapping("/{id}")
      public R<Dict> getById(@PathVariable Long id) {
            return R.ok(dictService.getById(id));
      }

      @GetMapping("/code/{dictCode}")
      public R<Dict> getByCode(@PathVariable String dictCode) {
            return R.ok(dictService.getByCode(dictCode));
      }

      @GetMapping("/{dictId}/items")
      public R<List<DictItem>> listDictItems(@PathVariable Long dictId) {
            return R.ok(dictService.listDictItems(dictId));
      }

      @PostMapping("/{dictId}/items")
      public R<DictItem> addDictItem(@PathVariable Long dictId, @RequestBody DictItem dictItem) {
            dictItem.setDictId(dictId);
            return R.ok(dictService.addDictItem(dictItem));
      }

      @PutMapping("/{dictId}/items")
      public R<DictItem> updateDictItem(@PathVariable Long dictId, @RequestBody DictItem dictItem) {
            dictItem.setDictId(dictId);
            return R.ok(dictService.updateDictItem(dictItem));
      }

      @DeleteMapping("/items/{id}")
      public R<Void> deleteDictItem(@PathVariable Long id) {
            dictService.deleteDictItem(id);
            return R.ok();
      }

      @PostMapping
      public R<Dict> addDict(@RequestBody Dict dict) {
            dictService.addDict(dict);
            return R.ok(dict);
      }

      @PutMapping
      public R<Dict> updateDict(@RequestBody Dict dict) {
            dictService.updateDict(dict);
            return R.ok(dict);
      }

      @DeleteMapping("/{id}")
      public R<Void> deleteDict(@PathVariable Long id) {
            dictService.deleteDict(id);
            return R.ok();
      }
}
