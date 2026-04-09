package com.ghlzm.iot.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.system.entity.Dict;
import com.ghlzm.iot.system.entity.DictItem;

import java.util.List;

public interface DictService extends IService<Dict> {

      Dict addDict(Dict dict);
      Dict addDict(Long currentUserId, Dict dict);

      List<Dict> listDicts(String dictName, String dictCode, String dictType);
      List<Dict> listDicts(Long currentUserId, String dictName, String dictCode, String dictType);

      PageResult<Dict> pageDicts(String dictName, String dictCode, String dictType, Long pageNum, Long pageSize);
      PageResult<Dict> pageDicts(Long currentUserId,
                                 String dictName,
                                 String dictCode,
                                 String dictType,
                                 Long pageNum,
                                 Long pageSize);

      List<Dict> listDictTree();
      List<Dict> listDictTree(Long currentUserId);

      Dict getById(Long currentUserId, Long id);

      Dict getByCode(String dictCode);
      Dict getByCode(Long currentUserId, String dictCode);

      List<DictItem> listDictItems(Long dictId);
      List<DictItem> listDictItems(Long currentUserId, Long dictId);

      DictItem addDictItem(DictItem dictItem);
      DictItem addDictItem(Long currentUserId, DictItem dictItem);

      DictItem updateDictItem(DictItem dictItem);
      DictItem updateDictItem(Long currentUserId, DictItem dictItem);

      void deleteDictItem(Long id);
      void deleteDictItem(Long currentUserId, Long id);

      void updateDict(Dict dict);
      void updateDict(Long currentUserId, Dict dict);

      void deleteDict(Long id);
      void deleteDict(Long currentUserId, Long id);
}
