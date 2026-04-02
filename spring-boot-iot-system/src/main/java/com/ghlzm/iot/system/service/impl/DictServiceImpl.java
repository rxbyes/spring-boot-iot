package com.ghlzm.iot.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.framework.mybatis.PageQueryUtils;
import com.ghlzm.iot.system.entity.Dict;
import com.ghlzm.iot.system.entity.DictItem;
import com.ghlzm.iot.system.mapper.DictItemMapper;
import com.ghlzm.iot.system.mapper.DictMapper;
import com.ghlzm.iot.system.service.DictService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class DictServiceImpl extends ServiceImpl<DictMapper, Dict> implements DictService {

      private final DictItemMapper dictItemMapper;

      public DictServiceImpl(DictItemMapper dictItemMapper) {
            this.dictItemMapper = dictItemMapper;
      }

      @Override
      @Transactional(rollbackFor = Exception.class)
      public Dict addDict(Dict dict) {
            LambdaQueryWrapper<Dict> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Dict::getTenantId, dict.getTenantId())
                    .eq(Dict::getDictCode, dict.getDictCode())
                    .eq(Dict::getDeleted, 0);
            if (this.count(queryWrapper) > 0) {
                  throw new BizException("字典编码已存在");
            }

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
      public List<Dict> listDicts(String dictName, String dictCode, String dictType) {
            return this.list(buildDictQueryWrapper(dictName, dictCode, dictType));
      }

      @Override
      public PageResult<Dict> pageDicts(String dictName, String dictCode, String dictType, Long pageNum, Long pageSize) {
            Page<Dict> page = PageQueryUtils.buildPage(pageNum, pageSize);
            Page<Dict> result = page(page, buildDictQueryWrapper(dictName, dictCode, dictType));
            return PageQueryUtils.toPageResult(result);
      }

      @Override
      public List<Dict> listDictTree() {
            return this.listDicts(null, null, null);
      }

      @Override
      public Dict getByCode(String dictCode) {
            LambdaQueryWrapper<Dict> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Dict::getDictCode, dictCode)
                    .eq(Dict::getDeleted, 0);
            Dict dict = this.getOne(queryWrapper);
            if (dict == null) {
                  return null;
            }
            LambdaQueryWrapper<DictItem> itemWrapper = new LambdaQueryWrapper<>();
            itemWrapper.eq(DictItem::getDictId, dict.getId())
                    .eq(DictItem::getDeleted, 0)
                    .eq(DictItem::getStatus, 1)
                    .orderByAsc(DictItem::getSortNo)
                    .orderByAsc(DictItem::getId);
            dict.setItems(dictItemMapper.selectList(itemWrapper));
            return dict;
      }

      @Override
      @Transactional(rollbackFor = Exception.class)
      public void updateDict(Dict dict) {
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

      private LambdaQueryWrapper<Dict> buildDictQueryWrapper(String dictName, String dictCode, String dictType) {
            LambdaQueryWrapper<Dict> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Dict::getDeleted, 0);
            if (StringUtils.hasText(dictName)) {
                  queryWrapper.like(Dict::getDictName, dictName.trim());
            }
            if (StringUtils.hasText(dictCode)) {
                  queryWrapper.like(Dict::getDictCode, dictCode.trim());
            }
            if (StringUtils.hasText(dictType)) {
                  queryWrapper.eq(Dict::getDictType, dictType.trim());
            }
            queryWrapper.orderByAsc(Dict::getSortNo).orderByAsc(Dict::getId);
            return queryWrapper;
      }
}
