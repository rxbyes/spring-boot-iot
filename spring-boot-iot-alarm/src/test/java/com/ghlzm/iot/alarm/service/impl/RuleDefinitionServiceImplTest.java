package com.ghlzm.iot.alarm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.LambdaUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ghlzm.iot.alarm.entity.RiskMetricCatalog;
import com.ghlzm.iot.alarm.entity.RuleDefinition;
import com.ghlzm.iot.alarm.service.RiskMetricCatalogService;
import com.ghlzm.iot.common.exception.BizException;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.mockito.ArgumentMatchers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RuleDefinitionServiceImplTest {

    @Mock
    private RiskMetricCatalogService riskMetricCatalogService;

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
        RuleDefinitionServiceImpl service = spy(new RuleDefinitionServiceImpl(riskMetricCatalogService));
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
        RuleDefinitionServiceImpl service = spy(new RuleDefinitionServiceImpl(riskMetricCatalogService));
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
        RuleDefinitionServiceImpl service = spy(new RuleDefinitionServiceImpl(riskMetricCatalogService));
        RuleDefinition rule = new RuleDefinition();
        rule.setRuleName("红色策略");
        rule.setMetricIdentifier("dispsX");
        rule.setAlarmLevel("critical");
        rule.setExpression("value >= 12");
        rule.setStatus(0);

        doReturn(true).when(service).save(ArgumentMatchers.any(RuleDefinition.class));

        service.addRule(rule);

        assertEquals("red", rule.getAlarmLevel());
        assertEquals(0, rule.getDeleted());
        verify(service).save(rule);
    }

    @Test
    void addRuleShouldRejectProductScopeWithoutProductId() {
        RuleDefinitionServiceImpl service = spy(new RuleDefinitionServiceImpl(riskMetricCatalogService));
        RuleDefinition rule = new RuleDefinition();
        rule.setRuleName("产品默认阈值");
        rule.setRuleScope("PRODUCT");
        rule.setMetricIdentifier("value");
        rule.setAlarmLevel("orange");
        rule.setExpression("value >= 8");
        rule.setStatus(0);

        BizException error = assertThrows(BizException.class, () -> service.addRule(rule));

        assertTrue(error.getMessage().contains("productId"));
        verify(service, never()).save(ArgumentMatchers.any(RuleDefinition.class));
    }

    @Test
    void addRuleShouldRejectDeviceScopeWithoutDeviceOrBindingId() {
        RuleDefinitionServiceImpl service = spy(new RuleDefinitionServiceImpl(riskMetricCatalogService));
        RuleDefinition rule = new RuleDefinition();
        rule.setRuleName("设备个性阈值");
        rule.setRuleScope("DEVICE");
        rule.setMetricIdentifier("value");
        rule.setAlarmLevel("red");
        rule.setExpression("value >= 10");
        rule.setStatus(0);

        BizException error = assertThrows(BizException.class, () -> service.addRule(rule));

        assertTrue(error.getMessage().contains("deviceId"));
        verify(service, never()).save(ArgumentMatchers.any(RuleDefinition.class));
    }

    @Test
    void addRuleShouldResolveMetricIdentifierFromRiskMetricCatalogWhenRiskMetricIdPresent() {
        RuleDefinitionServiceImpl service = spy(new RuleDefinitionServiceImpl(riskMetricCatalogService));
        RuleDefinition rule = new RuleDefinition();
        rule.setRuleName("GNSS 红色策略");
        rule.setRiskMetricId(6102L);
        rule.setAlarmLevel("critical");
        rule.setExpression("value >= 12");
        rule.setStatus(0);

        RiskMetricCatalog catalog = new RiskMetricCatalog();
        catalog.setId(6102L);
        catalog.setContractIdentifier("gpsTotalX");
        catalog.setRiskMetricName("GNSS 累计位移 X");
        when(riskMetricCatalogService.getById(6102L)).thenReturn(catalog);
        doReturn(true).when(service).save(ArgumentMatchers.any(RuleDefinition.class));

        service.addRule(rule);

        assertEquals("gpsTotalX", rule.getMetricIdentifier());
        assertEquals("GNSS 累计位移 X", rule.getMetricName());
        verify(service).save(rule);
    }

    @Test
    void addRuleShouldRejectRetiredRiskMetricCatalog() {
        RuleDefinitionServiceImpl service = spy(new RuleDefinitionServiceImpl(riskMetricCatalogService));
        RuleDefinition rule = new RuleDefinition();
        rule.setRuleName("已退役目录策略");
        rule.setRiskMetricId(6103L);
        rule.setExpression("value >= 12");
        rule.setStatus(0);

        RiskMetricCatalog catalog = new RiskMetricCatalog();
        catalog.setId(6103L);
        catalog.setContractIdentifier("gpsTotalY");
        catalog.setLifecycleStatus("RETIRED");
        when(riskMetricCatalogService.getById(6103L)).thenReturn(catalog);

        BizException error = assertThrows(BizException.class, () -> service.addRule(rule));

        assertEquals("风险指标目录当前不可绑定: 6103", error.getMessage());
        verify(service, never()).save(any(RuleDefinition.class));
    }

    @Test
    void pageRuleListShouldApplyRuleNameLikeFilter() {
        initLambdaCache();
        RuleDefinitionServiceImpl service = spy(new RuleDefinitionServiceImpl(riskMetricCatalogService));
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
    void pageRuleListShouldTreatOrangeFilterAsCompatibleWithLegacyWarningSeverity() {
        initLambdaCache();
        RuleDefinitionServiceImpl service = spy(new RuleDefinitionServiceImpl(riskMetricCatalogService));
        Page<RuleDefinition> page = new Page<>(1L, 10L);
        page.setRecords(java.util.List.of());
        page.setTotal(0L);
        doReturn(page).when(service).page(any(Page.class), any(LambdaQueryWrapper.class));

        service.pageRuleList(null, null, "orange", null, 1L, 10L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<RuleDefinition>> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(service).page(any(Page.class), wrapperCaptor.capture());
        LambdaQueryWrapper<RuleDefinition> wrapper = wrapperCaptor.getValue();
        wrapper.getSqlSegment();
        assertTrue(wrapper.getParamNameValuePairs().values().contains("orange"));
        assertTrue(wrapper.getParamNameValuePairs().values().contains("warning"));
    }

    @Test
    void pageRuleListShouldFilterByProductDefaultScopeAndProduct() {
        initLambdaCache();
        RuleDefinitionServiceImpl service = spy(new RuleDefinitionServiceImpl(riskMetricCatalogService));
        Page<RuleDefinition> page = new Page<>(1L, 10L);
        page.setRecords(java.util.List.of());
        page.setTotal(0L);
        doReturn(page).when(service).page(any(Page.class), any(LambdaQueryWrapper.class));

        service.pageRuleList(null, "value", null, null, "product", 1001L, 1L, 10L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<RuleDefinition>> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(service).page(any(Page.class), wrapperCaptor.capture());
        LambdaQueryWrapper<RuleDefinition> wrapper = wrapperCaptor.getValue();
        wrapper.getSqlSegment();
        assertTrue(wrapper.getParamNameValuePairs().values().contains("value"));
        assertTrue(wrapper.getParamNameValuePairs().values().contains("PRODUCT"));
        assertTrue(wrapper.getParamNameValuePairs().values().contains(1001L));
    }
}
