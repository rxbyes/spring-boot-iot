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
import com.ghlzm.iot.system.service.PermissionService;
import com.ghlzm.iot.system.service.model.DataPermissionContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class DictServiceImpl extends ServiceImpl<DictMapper, Dict> implements DictService {

      private final DictItemMapper dictItemMapper;
      private final PermissionService permissionService;

      public DictServiceImpl(DictItemMapper dictItemMapper,
                             PermissionService permissionService) {
            this.dictItemMapper = dictItemMapper;
            this.permissionService = permissionService;
      }

      @Override
      @Transactional(rollbackFor = Exception.class)
      public Dict addDict(Dict dict) {
            return addDict(null, dict);
      }

      @Override
      @Transactional(rollbackFor = Exception.class)
      public Dict addDict(Long currentUserId, Dict dict) {
            Long tenantId = resolveTenantId(currentUserId, dict.getTenantId());
            LambdaQueryWrapper<Dict> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Dict::getTenantId, tenantId)
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
            dict.setTenantId(tenantId == null ? 1L : tenantId);
            if (dict.getCreateBy() == null) {
                  dict.setCreateBy(currentUserId == null ? 1L : currentUserId);
            }

            this.save(dict);
            return dict;
      }

      @Override
      public List<Dict> listDicts(String dictName, String dictCode, String dictType) {
            return listDicts(null, dictName, dictCode, dictType);
      }

      @Override
      public List<Dict> listDicts(Long currentUserId, String dictName, String dictCode, String dictType) {
            return this.list(buildDictQueryWrapper(currentUserId, dictName, dictCode, dictType));
      }

      @Override
      public PageResult<Dict> pageDicts(String dictName, String dictCode, String dictType, Long pageNum, Long pageSize) {
            return pageDicts(null, dictName, dictCode, dictType, pageNum, pageSize);
      }

      @Override
      public PageResult<Dict> pageDicts(Long currentUserId,
                                        String dictName,
                                        String dictCode,
                                        String dictType,
                                        Long pageNum,
                                        Long pageSize) {
            Page<Dict> page = PageQueryUtils.buildPage(pageNum, pageSize);
            Page<Dict> result = page(page, buildDictQueryWrapper(currentUserId, dictName, dictCode, dictType));
            return PageQueryUtils.toPageResult(result);
      }

      @Override
      public List<Dict> listDictTree() {
            return listDictTree(null);
      }

      @Override
      public List<Dict> listDictTree(Long currentUserId) {
            return this.listDicts(currentUserId, null, null, null);
      }

      @Override
      public Dict getById(Long currentUserId, Long id) {
            Dict dict = super.getById(id);
            if (dict == null) {
                  return null;
            }
            ensureDictAccessible(currentUserId, dict);
            return dict;
      }

      @Override
      public Dict getByCode(String dictCode) {
            return getByCode(null, dictCode);
      }

      @Override
      public Dict getByCode(Long currentUserId, String dictCode) {
            LambdaQueryWrapper<Dict> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Dict::getDictCode, dictCode)
                    .eq(resolveTenantId(currentUserId, null) != null, Dict::getTenantId, resolveTenantId(currentUserId, null))
                    .eq(Dict::getDeleted, 0);
            Dict dict = this.getOne(queryWrapper);
            if (dict == null) {
                  return null;
            }
            ensureDictAccessible(currentUserId, dict);
            dict.setItems(listEnabledDictItems(dict.getId(), dict.getTenantId()));
            return dict;
      }

      @Override
      public List<DictItem> listDictItems(Long dictId) {
            return listDictItems(null, dictId);
      }

      @Override
      public List<DictItem> listDictItems(Long currentUserId, Long dictId) {
            Dict dict = requireDict(dictId);
            ensureDictAccessible(currentUserId, dict);
            LambdaQueryWrapper<DictItem> itemWrapper = new LambdaQueryWrapper<>();
            itemWrapper.eq(DictItem::getDictId, dictId)
                    .eq(DictItem::getTenantId, dict.getTenantId())
                    .eq(DictItem::getDeleted, 0)
                    .orderByAsc(DictItem::getSortNo)
                    .orderByAsc(DictItem::getId);
            return dictItemMapper.selectList(itemWrapper);
      }

      @Override
      @Transactional(rollbackFor = Exception.class)
      public DictItem addDictItem(DictItem dictItem) {
            return addDictItem(null, dictItem);
      }

      @Override
      @Transactional(rollbackFor = Exception.class)
      public DictItem addDictItem(Long currentUserId, DictItem dictItem) {
            Dict dict = requireDict(dictItem.getDictId());
            ensureDictAccessible(currentUserId, dict);
            Long tenantId = dict.getTenantId();
            ensureUniqueItemValue(tenantId, dictItem.getDictId(), dictItem.getItemValue(), null);
            if (dictItem.getSortNo() == null) {
                  dictItem.setSortNo(0);
            }
            if (dictItem.getStatus() == null) {
                  dictItem.setStatus(1);
            }
            if (dictItem.getDeleted() == null) {
                  dictItem.setDeleted(0);
            }
            dictItem.setTenantId(tenantId);
            dictItemMapper.insert(dictItem);
            return dictItem;
      }

      @Override
      @Transactional(rollbackFor = Exception.class)
      public DictItem updateDictItem(DictItem dictItem) {
            return updateDictItem(null, dictItem);
      }

      @Override
      @Transactional(rollbackFor = Exception.class)
      public DictItem updateDictItem(Long currentUserId, DictItem dictItem) {
            DictItem existing = dictItemMapper.selectById(dictItem.getId());
            if (existing == null || Integer.valueOf(1).equals(existing.getDeleted())) {
                  throw new BizException("字典项不存在");
            }
            Dict dict = requireDict(dictItem.getDictId());
            ensureDictAccessible(currentUserId, dict);
            ensureUniqueItemValue(existing.getTenantId(), dict.getId(), dictItem.getItemValue(), existing.getId());
            existing.setDictId(dict.getId());
            existing.setTenantId(dict.getTenantId());
            existing.setItemName(dictItem.getItemName());
            existing.setItemValue(dictItem.getItemValue());
            existing.setItemType(dictItem.getItemType());
            existing.setStatus(dictItem.getStatus());
            existing.setSortNo(dictItem.getSortNo());
            existing.setRemark(dictItem.getRemark());
            dictItemMapper.updateById(existing);
            return existing;
      }

      @Override
      @Transactional(rollbackFor = Exception.class)
      public void deleteDictItem(Long id) {
            deleteDictItem(null, id);
      }

      @Override
      @Transactional(rollbackFor = Exception.class)
      public void deleteDictItem(Long currentUserId, Long id) {
            if (currentUserId != null) {
                  DictItem existing = dictItemMapper.selectById(id);
                  if (existing == null || Integer.valueOf(1).equals(existing.getDeleted())) {
                        throw new BizException("字典项不存在");
                  }
                  Dict dict = requireDict(existing.getDictId());
                  ensureDictAccessible(currentUserId, dict);
            }
            dictItemMapper.deleteById(id);
      }

      @Override
      @Transactional(rollbackFor = Exception.class)
      public void updateDict(Dict dict) {
            updateDict(null, dict);
      }

      @Override
      @Transactional(rollbackFor = Exception.class)
      public void updateDict(Long currentUserId, Dict dict) {
            Dict existing = super.getById(dict.getId());
            if (existing == null || Integer.valueOf(1).equals(existing.getDeleted())) {
                  throw new BizException("字典不存在");
            }
            ensureDictAccessible(currentUserId, existing);
            LambdaQueryWrapper<Dict> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Dict::getTenantId, existing.getTenantId())
                    .eq(Dict::getDictCode, dict.getDictCode())
                    .ne(Dict::getId, dict.getId())
                    .eq(Dict::getDeleted, 0);
            if (this.count(queryWrapper) > 0) {
                  throw new BizException("字典编码已存在");
            }

            dict.setTenantId(existing.getTenantId());
            if (dict.getUpdateBy() == null && currentUserId != null) {
                  dict.setUpdateBy(currentUserId);
            }
            this.updateById(dict);
      }

      @Override
      @Transactional(rollbackFor = Exception.class)
      public void deleteDict(Long id) {
            deleteDict(null, id);
      }

      @Override
      @Transactional(rollbackFor = Exception.class)
      public void deleteDict(Long currentUserId, Long id) {
            Dict existing = super.getById(id);
            if (existing == null || Integer.valueOf(1).equals(existing.getDeleted())) {
                  throw new BizException("字典不存在");
            }
            ensureDictAccessible(currentUserId, existing);
            this.removeById(id);
      }

      private LambdaQueryWrapper<Dict> buildDictQueryWrapper(Long currentUserId,
                                                            String dictName,
                                                            String dictCode,
                                                            String dictType) {
            LambdaQueryWrapper<Dict> queryWrapper = new LambdaQueryWrapper<>();
            Long tenantId = resolveTenantId(currentUserId, null);
            queryWrapper.eq(tenantId != null, Dict::getTenantId, tenantId);
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

      private List<DictItem> listEnabledDictItems(Long dictId, Long tenantId) {
            LambdaQueryWrapper<DictItem> itemWrapper = new LambdaQueryWrapper<>();
            itemWrapper.eq(DictItem::getDictId, dictId)
                    .eq(tenantId != null, DictItem::getTenantId, tenantId)
                    .eq(DictItem::getDeleted, 0)
                    .eq(DictItem::getStatus, 1)
                    .orderByAsc(DictItem::getSortNo)
                    .orderByAsc(DictItem::getId);
            return dictItemMapper.selectList(itemWrapper);
      }

      private Dict requireDict(Long dictId) {
            Dict dict = this.getById(dictId);
            if (dict == null || Integer.valueOf(1).equals(dict.getDeleted())) {
                  throw new BizException("字典不存在");
            }
            return dict;
      }

      private void ensureUniqueItemValue(Long tenantId, Long dictId, String itemValue, Long excludeId) {
            LambdaQueryWrapper<DictItem> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(DictItem::getTenantId, tenantId)
                    .eq(DictItem::getDictId, dictId)
                    .eq(DictItem::getItemValue, itemValue)
                    .eq(DictItem::getDeleted, 0);
            if (excludeId != null) {
                  queryWrapper.ne(DictItem::getId, excludeId);
            }
            if (dictItemMapper.selectCount(queryWrapper) > 0) {
                  throw new BizException("字典项值已存在");
            }
      }

      private Long resolveTenantId(Long currentUserId, Long fallbackTenantId) {
            if (currentUserId == null) {
                  return fallbackTenantId;
            }
            return permissionService.getDataPermissionContext(currentUserId).tenantId();
      }

      private void ensureDictAccessible(Long currentUserId, Dict dict) {
            if (currentUserId == null || dict == null) {
                  return;
            }
            DataPermissionContext context = permissionService.getDataPermissionContext(currentUserId);
            if (context.superAdmin()) {
                  return;
            }
            if (context.tenantId() != null && !context.tenantId().equals(dict.getTenantId())) {
                  throw new BizException("字典不存在或无权访问");
            }
      }
}
