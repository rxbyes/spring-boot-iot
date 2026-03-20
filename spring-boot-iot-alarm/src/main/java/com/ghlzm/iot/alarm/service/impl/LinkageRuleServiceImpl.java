package com.ghlzm.iot.alarm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ghlzm.iot.alarm.entity.LinkageRule;
import com.ghlzm.iot.alarm.mapper.LinkageRuleMapper;
import com.ghlzm.iot.alarm.service.LinkageRuleService;
import com.ghlzm.iot.common.response.PageResult;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 联动规则Service实现类
 */
@Service
public class LinkageRuleServiceImpl extends ServiceImpl<LinkageRuleMapper, LinkageRule> implements LinkageRuleService {

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
      public void addRule(LinkageRule rule) {
            rule.setDeleted(0);
            save(rule);
      }

      @Override
      public void updateRule(LinkageRule rule) {
            updateById(rule);
      }

      @Override
      public void deleteRule(Long id) {
            removeById(id);
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
