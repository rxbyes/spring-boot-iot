package com.ghlzm.iot.alarm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ghlzm.iot.alarm.entity.LinkageRule;
import com.ghlzm.iot.alarm.mapper.LinkageRuleMapper;
import com.ghlzm.iot.alarm.service.LinkageRuleService;
import com.ghlzm.iot.alarm.service.RiskMetricActionBindingSyncService;
import com.ghlzm.iot.common.response.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 联动规则Service实现类
 */
@Service
public class LinkageRuleServiceImpl extends ServiceImpl<LinkageRuleMapper, LinkageRule> implements LinkageRuleService {

      private final RiskMetricActionBindingSyncService bindingSyncService;

      public LinkageRuleServiceImpl(RiskMetricActionBindingSyncService bindingSyncService) {
            this.bindingSyncService = bindingSyncService;
      }

      @Override
      public PageResult<LinkageRule> pageRuleList(String ruleName, Integer status, Long pageNum, Long pageSize) {
            Page<LinkageRule> page = new Page<>(pageNum, pageSize);
            Page<LinkageRule> result = page(page, buildWrapper(ruleName, status));
            return PageResult.of(result.getTotal(), pageNum, pageSize, result.getRecords());
      }

      @Override
      public List<LinkageRule> getRuleList(String ruleName, Integer status) {
            return list(buildWrapper(ruleName, status));
      }

      @Override
      @Transactional(rollbackFor = Exception.class)
      public void addRule(LinkageRule rule, Long operatorId) {
            rule.setDeleted(0);
            rule.setCreateBy(operatorId);
            rule.setUpdateBy(operatorId);
            save(rule);
            bindingSyncService.rebuildLinkageBindingsForRule(rule, operatorId, "AUTO_INFERRED");
      }

      @Override
      @Transactional(rollbackFor = Exception.class)
      public void updateRule(LinkageRule rule, Long operatorId) {
            rule.setUpdateBy(operatorId);
            updateById(rule);
            bindingSyncService.rebuildLinkageBindingsForRule(rule, operatorId, "AUTO_INFERRED");
      }

      @Override
      @Transactional(rollbackFor = Exception.class)
      public void deleteRule(Long id, Long operatorId) {
            removeById(id);
            bindingSyncService.deactivateLinkageBindings(id, operatorId);
      }

      private LambdaQueryWrapper<LinkageRule> buildWrapper(String ruleName, Integer status) {
            LambdaQueryWrapper<LinkageRule> wrapper = new LambdaQueryWrapper<>();
            if (ruleName != null && !ruleName.isEmpty()) {
                  wrapper.eq(LinkageRule::getRuleName, ruleName);
            }
            if (status != null) {
                  wrapper.eq(LinkageRule::getStatus, status);
            }
            wrapper.eq(LinkageRule::getDeleted, 0);
            wrapper.orderByDesc(LinkageRule::getCreateTime);
            return wrapper;
      }
}
