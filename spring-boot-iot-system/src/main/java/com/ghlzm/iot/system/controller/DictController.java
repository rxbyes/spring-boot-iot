package com.ghlzm.iot.system.controller;

import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.framework.security.JwtUserPrincipal;
import com.ghlzm.iot.system.entity.Dict;
import com.ghlzm.iot.system.entity.DictItem;
import com.ghlzm.iot.system.service.DictService;
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
@RequestMapping("/api/dict")
public class DictController {

      private final DictService dictService;

      public DictController(DictService dictService) {
            this.dictService = dictService;
      }

      @GetMapping("/list")
      public R<List<Dict>> listDicts(@RequestParam(required = false) String dictName,
                                     @RequestParam(required = false) String dictCode,
                                     @RequestParam(required = false) String dictType,
                                     Authentication authentication) {
            return R.ok(dictService.listDicts(requireCurrentUserId(authentication), dictName, dictCode, dictType));
      }

      @GetMapping("/page")
      public R<PageResult<Dict>> pageDicts(@RequestParam(required = false) String dictName,
                                           @RequestParam(required = false) String dictCode,
                                           @RequestParam(required = false) String dictType,
                                           @RequestParam(defaultValue = "1") Long pageNum,
                                           @RequestParam(defaultValue = "10") Long pageSize,
                                           Authentication authentication) {
            return R.ok(dictService.pageDicts(requireCurrentUserId(authentication), dictName, dictCode, dictType, pageNum, pageSize));
      }

      @GetMapping("/tree")
      public R<List<Dict>> listDictTree(Authentication authentication) {
            return R.ok(dictService.listDictTree(requireCurrentUserId(authentication)));
      }

      @GetMapping("/{id}")
      public R<Dict> getById(@PathVariable Long id, Authentication authentication) {
            return R.ok(dictService.getById(requireCurrentUserId(authentication), id));
      }

      @GetMapping("/code/{dictCode}")
      public R<Dict> getByCode(@PathVariable String dictCode, Authentication authentication) {
            return R.ok(dictService.getByCode(requireCurrentUserId(authentication), dictCode));
      }

      @GetMapping("/{dictId}/items")
      public R<List<DictItem>> listDictItems(@PathVariable Long dictId, Authentication authentication) {
            return R.ok(dictService.listDictItems(requireCurrentUserId(authentication), dictId));
      }

      @PostMapping("/{dictId}/items")
      public R<DictItem> addDictItem(@PathVariable Long dictId,
                                     @RequestBody DictItem dictItem,
                                     Authentication authentication) {
            dictItem.setDictId(dictId);
            return R.ok(dictService.addDictItem(requireCurrentUserId(authentication), dictItem));
      }

      @PutMapping("/{dictId}/items")
      public R<DictItem> updateDictItem(@PathVariable Long dictId,
                                        @RequestBody DictItem dictItem,
                                        Authentication authentication) {
            dictItem.setDictId(dictId);
            return R.ok(dictService.updateDictItem(requireCurrentUserId(authentication), dictItem));
      }

      @DeleteMapping("/items/{id}")
      public R<Void> deleteDictItem(@PathVariable Long id, Authentication authentication) {
            dictService.deleteDictItem(requireCurrentUserId(authentication), id);
            return R.ok();
      }

      @PostMapping
      public R<Dict> addDict(@RequestBody Dict dict, Authentication authentication) {
            dictService.addDict(requireCurrentUserId(authentication), dict);
            return R.ok(dict);
      }

      @PutMapping
      public R<Dict> updateDict(@RequestBody Dict dict, Authentication authentication) {
            dictService.updateDict(requireCurrentUserId(authentication), dict);
            return R.ok(dict);
      }

      @DeleteMapping("/{id}")
      public R<Void> deleteDict(@PathVariable Long id, Authentication authentication) {
            dictService.deleteDict(requireCurrentUserId(authentication), id);
            return R.ok();
      }

      private Long requireCurrentUserId(Authentication authentication) {
            if (authentication == null || !(authentication.getPrincipal() instanceof JwtUserPrincipal principal)) {
                  throw new com.ghlzm.iot.common.exception.BizException(401, "未认证，请先登录");
            }
            return principal.userId();
      }
}
