package com.ghlzm.iot.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ghlzm.iot.system.entity.Dict;

import java.util.List;

/**
 * 字典配置 Service
 */
public interface DictService extends IService<Dict> {

      /**
       * 添加字典
       */
      Dict addDict(Dict dict);

      /**
       * 查询字典列表
       */
      List<Dict> listDicts();

      /**
       * 查询字典树
       */
      List<Dict> listDictTree();

      /**
       * 根据字典编码查询字典
       */
      Dict getByCode(String dictCode);

      /**
       * 更新字典
       */
      void updateDict(Dict dict);

      /**
       * 删除字典
       */
      void deleteDict(Long id);
}
