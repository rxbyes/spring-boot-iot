package com.ghlzm.iot.system.controller;

import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.system.entity.Dict;
import com.ghlzm.iot.system.service.DictService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 字典配置 Controller
 */
@RestController
@RequestMapping("/api/dict")
public class DictController {

      @Autowired
      private DictService dictService;

      /**
       * 查询字典列表
       */
      @GetMapping("/list")
      public R<List<Dict>> listDicts() {
            List<Dict> dicts = dictService.listDicts();
            return R.ok(dicts);
      }

      /**
       * 查询字典树
       */
      @GetMapping("/tree")
      public R<List<Dict>> listDictTree() {
            List<Dict> dicts = dictService.listDictTree();
            return R.ok(dicts);
      }

      /**
       * 根据ID查询字典
       */
      @GetMapping("/{id}")
      public R<Dict> getById(@PathVariable Long id) {
            Dict dict = dictService.getById(id);
            return R.ok(dict);
      }

      /**
       * 根据编码查询字典
       */
      @GetMapping("/code/{dictCode}")
      public R<Dict> getByCode(@PathVariable String dictCode) {
            Dict dict = dictService.getByCode(dictCode);
            return R.ok(dict);
      }

      /**
       * 添加字典
       */
      @PostMapping
      public R<Dict> addDict(@RequestBody Dict dict) {
            dictService.addDict(dict);
            return R.ok(dict);
      }

      /**
       * 更新字典
       */
      @PutMapping
      public R<Dict> updateDict(@RequestBody Dict dict) {
            dictService.updateDict(dict);
            return R.ok(dict);
      }

      /**
       * 删除字典
       */
      @DeleteMapping("/{id}")
      public R<Void> deleteDict(@PathVariable Long id) {
            dictService.deleteDict(id);
            return R.ok();
      }
}
