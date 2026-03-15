package com.ghlzm.iot.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.system.entity.Dict;
import com.ghlzm.iot.system.mapper.DictMapper;
import com.ghlzm.iot.system.service.DictService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 字典配置 Service 实现类
 */
@Service
public class DictServiceImpl extends ServiceImpl<DictMapper, Dict>
            implements DictService {

      @Override
      @Transactional(rollbackFor = Exception.class)
      public Dict addDict(Dict dict) {
            // 验证字典编码唯一性
            LambdaQueryWrapper<Dict> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Dict::getTenantId, dict.getTenantId())
                        .eq(Dict::getDictCode, dict.getDictCode())
                        .eq(Dict::getDeleted, 0);
            if (this.count(queryWrapper) > 0) {
                  throw new BizException("字典编码已存在");
            }

            // 设置默认值
            if (dict.getSortNo() == null) {
                  dict.setSortNo(0);
            }
            if (dict.getStatus() == null) {
                  dict.setStatus(1);
            }

            this.save(dict);
            return dict;
      }

      @Override
      public List<Dict> listDicts() {
            LambdaQueryWrapper<Dict> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Dict::getDeleted, 0);
            queryWrapper.orderByAsc(Dict::getSortNo);
            return this.list(queryWrapper);
      }

      @Override
      public List<Dict> listDictTree() {
            return this.listDicts();
      }

      @Override
      public Dict getByCode(String dictCode) {
            LambdaQueryWrapper<Dict> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Dict::getDictCode, dictCode)
                        .eq(Dict::getDeleted, 0);
            return this.getOne(queryWrapper);
      }

      @Override
      @Transactional(rollbackFor = Exception.class)
      public void updateDict(Dict dict) {
            // 验证字典编码唯一性
            LambdaQueryWrapper<Dict> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Dict::getTenantId, dict.getTenantId())
                        .eq(Dict::getDictCode, dict.getDictCode())
                        .ne(Dict::getId, dict.getId())
                        .eq(Dict::getDeleted, 0);
            if (this.count(queryWrapper) > 0) {
                  throw new BizException("字典编码已存在");
            }

            this.updateById(dict);
      }

      @Override
      @Transactional(rollbackFor = Exception.class)
      public void deleteDict(Long id) {
            this.removeById(id);
      }
}
