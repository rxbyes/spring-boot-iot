package com.ghlzm.iot.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.system.entity.Dict;
import com.ghlzm.iot.system.entity.DictItem;

import java.util.List;

public interface DictService extends IService<Dict> {

      Dict addDict(Dict dict);

      List<Dict> listDicts(String dictName, String dictCode, String dictType);

      PageResult<Dict> pageDicts(String dictName, String dictCode, String dictType, Long pageNum, Long pageSize);

      List<Dict> listDictTree();

      Dict getByCode(String dictCode);

      List<DictItem> listDictItems(Long dictId);

      DictItem addDictItem(DictItem dictItem);

      DictItem updateDictItem(DictItem dictItem);

      void deleteDictItem(Long id);

      void updateDict(Dict dict);

      void deleteDict(Long id);
}
