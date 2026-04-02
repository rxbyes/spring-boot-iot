package com.ghlzm.iot.alarm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.LambdaUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ghlzm.iot.alarm.entity.RuleDefinition;
import com.ghlzm.iot.common.exception.BizException;
import org.junit.jupiter.api.Test;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.mockito.ArgumentMatchers;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class RuleDefinitionServiceImplTest {

    private static void initLambdaCache() {
        if (TableInfoHelper.getTableInfo(RuleDefinition.class) != null) {
            return;
        }
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new Configuration(), "");
        assistant.setCurrentNamespace(RuleDefinition.class.getName());
        LambdaUtils.installCache(TableInfoHelper.initTableInfo(assistant, RuleDefinition.class));
    }

    @Test
    void addRuleShouldRejectEnabledRuleWithoutExpression() {
        RuleDefinitionServiceImpl service = spy(new RuleDefinitionServiceImpl());
        RuleDefinition rule = new RuleDefinition();
        rule.setRuleName("空表达式策略");
        rule.setMetricIdentifier("dispsX");
        rule.setStatus(0);

        BizException error = assertThrows(BizException.class, () -> service.addRule(rule));

        assertEquals("启用中的阈值策略必须提供可执行表达式", error.getMessage());
        verify(service, never()).save(ArgumentMatchers.any(RuleDefinition.class));
    }

    @Test
    void updateRuleShouldRejectEnabledRuleWithInvalidExpression() {
        RuleDefinitionServiceImpl service = spy(new RuleDefinitionServiceImpl());
        RuleDefinition rule = new RuleDefinition();
        rule.setId(9102L);
        rule.setRuleName("非法表达式策略");
        rule.setMetricIdentifier("dispsX");
        rule.setExpression("value >< 5");
        rule.setStatus(0);

        BizException error = assertThrows(BizException.class, () -> service.updateRule(rule));

        assertEquals("阈值策略表达式格式无效，仅支持 value >= 12 这类写法", error.getMessage());
        verify(service, never()).updateById(ArgumentMatchers.any(RuleDefinition.class));
    }

    @Test
    void addRuleShouldPersistWhenExpressionIsExecutable() {
        RuleDefinitionServiceImpl service = spy(new RuleDefinitionServiceImpl());
        RuleDefinition rule = new RuleDefinition();
        rule.setRuleName("红色策略");
        rule.setMetricIdentifier("dispsX");
        rule.setExpression("value >= 12");
        rule.setStatus(0);

        doReturn(true).when(service).save(ArgumentMatchers.any(RuleDefinition.class));

        service.addRule(rule);

        assertEquals(0, rule.getDeleted());
        verify(service).save(rule);
    }

    @Test
    void pageRuleListShouldApplyRuleNameLikeFilter() {
        initLambdaCache();
        RuleDefinitionServiceImpl service = spy(new RuleDefinitionServiceImpl());
        Page<RuleDefinition> page = new Page<>(1L, 10L);
        page.setRecords(java.util.List.of());
        page.setTotal(0L);
        doReturn(page).when(service).page(any(Page.class), any(LambdaQueryWrapper.class));

        service.pageRuleList("边坡", null, null, null, 1L, 10L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<RuleDefinition>> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(service).page(any(Page.class), wrapperCaptor.capture());
        LambdaQueryWrapper<RuleDefinition> wrapper = wrapperCaptor.getValue();
        wrapper.getSqlSegment();
        assertTrue(wrapper.getParamNameValuePairs().values().stream().anyMatch(value -> String.valueOf(value).contains("边坡")));
    }

    @Test
    void pageRuleListShouldTreatWarningFilterAsCompatibleWithOrangeSeverity() {
        initLambdaCache();
        RuleDefinitionServiceImpl service = spy(new RuleDefinitionServiceImpl());
        Page<RuleDefinition> page = new Page<>(1L, 10L);
        page.setRecords(java.util.List.of());
        page.setTotal(0L);
        doReturn(page).when(service).page(any(Page.class), any(LambdaQueryWrapper.class));

        service.pageRuleList(null, null, "warning", null, 1L, 10L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<RuleDefinition>> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(service).page(any(Page.class), wrapperCaptor.capture());
        LambdaQueryWrapper<RuleDefinition> wrapper = wrapperCaptor.getValue();
        wrapper.getSqlSegment();
        assertTrue(wrapper.getParamNameValuePairs().values().contains("warning"));
        assertTrue(wrapper.getParamNameValuePairs().values().contains("orange"));
    }
}
