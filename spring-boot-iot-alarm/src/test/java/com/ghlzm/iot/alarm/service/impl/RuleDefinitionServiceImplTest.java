package com.ghlzm.iot.alarm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.LambdaUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ghlzm.iot.alarm.entity.RiskMetricCatalog;
import com.ghlzm.iot.alarm.entity.RuleDefinition;
import com.ghlzm.iot.alarm.service.RiskMetricCatalogService;
import com.ghlzm.iot.alarm.vo.RuleDefinitionBatchAddResultVO;
import com.ghlzm.iot.alarm.vo.RuleDefinitionEffectivePreviewVO;
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

        doReturn(0L).when(service).count(any(LambdaQueryWrapper.class));
        doReturn(true).when(service).save(ArgumentMatchers.any(RuleDefinition.class));

        service.addRule(rule);

        assertEquals("red", rule.getAlarmLevel());
        assertEquals(0, rule.getDeleted());
        verify(service).save(rule);
    }

    @Test
    void updateRuleShouldAllowExpressionChangeWhenPolicyIdentityIsUnchanged() {
        RuleDefinitionServiceImpl service = spy(new RuleDefinitionServiceImpl(riskMetricCatalogService));
        RuleDefinition existing = new RuleDefinition();
        existing.setId(8249L);
        existing.setRuleName("rain product threshold");
        existing.setRuleScope("PRODUCT");
        existing.setProductId(1001L);
        existing.setMetricIdentifier("value");
        existing.setAlarmLevel("orange");
        existing.setExpression("value >= 0.1");
        existing.setStatus(0);
        RuleDefinition rule = new RuleDefinition();
        rule.setId(8249L);
        rule.setRuleName("rain product threshold");
        rule.setRuleScope("PRODUCT");
        rule.setProductId(1001L);
        rule.setMetricIdentifier("value");
        rule.setAlarmLevel("orange");
        rule.setExpression("value >= 0.2");
        rule.setStatus(0);
        doReturn(existing).when(service).getById(8249L);
        doReturn(true).when(service).updateById(ArgumentMatchers.any(RuleDefinition.class));

        service.updateRule(rule);

        verify(service, never()).count(any(LambdaQueryWrapper.class));
        verify(service).updateById(rule);
    }

    @Test
    void batchAddRulesShouldPersistValidRulesAndReportInvalidOnes() {
        RuleDefinitionServiceImpl service = spy(new RuleDefinitionServiceImpl(riskMetricCatalogService));
        RuleDefinition validRule = new RuleDefinition();
        validRule.setRuleName("product default threshold");
        validRule.setRuleScope("PRODUCT");
        validRule.setProductId(1001L);
        validRule.setMetricIdentifier("value");
        validRule.setAlarmLevel("warning");
        validRule.setExpression("value >= 8");
        validRule.setStatus(0);
        RuleDefinition invalidRule = new RuleDefinition();
        invalidRule.setRuleName("invalid empty expression threshold");
        invalidRule.setRuleScope("PRODUCT");
        invalidRule.setProductId(1001L);
        invalidRule.setMetricIdentifier("value");
        invalidRule.setStatus(0);
        doReturn(0L).when(service).count(any(LambdaQueryWrapper.class));
        doReturn(true).when(service).save(ArgumentMatchers.any(RuleDefinition.class));

        RuleDefinitionBatchAddResultVO result = service.batchAddRules(java.util.List.of(validRule, invalidRule));

        assertEquals(2, result.getTotalCount());
        assertEquals(1, result.getSuccessCount());
        assertEquals(1, result.getFailedCount());
        assertEquals(Boolean.TRUE, result.getItems().get(0).getSuccess());
        assertEquals(Boolean.FALSE, result.getItems().get(1).getSuccess());
        assertEquals("orange", validRule.getAlarmLevel());
        verify(service).save(validRule);
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
    void addRuleShouldRejectProductTypeScopeWithoutProductType() {
        RuleDefinitionServiceImpl service = spy(new RuleDefinitionServiceImpl(riskMetricCatalogService));
        RuleDefinition rule = new RuleDefinition();
        rule.setRuleName("monitoring product type threshold");
        rule.setRuleScope("PRODUCT_TYPE");
        rule.setMetricIdentifier("value");
        rule.setAlarmLevel("orange");
        rule.setExpression("value >= 8");
        rule.setStatus(0);

        BizException error = assertThrows(BizException.class, () -> service.addRule(rule));

        assertTrue(error.getMessage().contains("productType"));
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
        doReturn(0L).when(service).count(any(LambdaQueryWrapper.class));
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
    void addRuleShouldRejectDuplicateRuleInSameScopeAndMetric() {
        RuleDefinitionServiceImpl service = spy(new RuleDefinitionServiceImpl(riskMetricCatalogService));
        RuleDefinition rule = new RuleDefinition();
        rule.setRuleName("monitoring template threshold");
        rule.setRuleScope("PRODUCT_TYPE");
        rule.setProductType("monitoring");
        rule.setMetricIdentifier("value");
        rule.setAlarmLevel("orange");
        rule.setExpression("value >= 8");
        rule.setStatus(0);

        doReturn(1L).when(service).count(any(LambdaQueryWrapper.class));

        BizException error = assertThrows(BizException.class, () -> service.addRule(rule));

        assertTrue(error.getMessage().contains("已存在相同范围和测点的启用阈值策略"));
        assertTrue(error.getMessage().contains("产品类型模板"));
        assertEquals("PRODUCT_TYPE", rule.getRuleScope());
        assertEquals("MONITORING", rule.getProductType());
        verify(service, never()).save(any(RuleDefinition.class));
    }

    @Test
    void addRuleShouldIgnoreDisabledDuplicatesWhenCheckingActivePolicy() {
        RuleDefinitionServiceImpl service = spy(new RuleDefinitionServiceImpl(riskMetricCatalogService));
        RuleDefinition rule = new RuleDefinition();
        rule.setRuleName("product threshold");
        rule.setRuleScope("PRODUCT");
        rule.setProductId(1001L);
        rule.setMetricIdentifier("value");
        rule.setAlarmLevel("orange");
        rule.setExpression("value >= 8");
        rule.setStatus(0);

        doReturn(0L).when(service).count(any(LambdaQueryWrapper.class));
        doReturn(true).when(service).save(ArgumentMatchers.any(RuleDefinition.class));

        service.addRule(rule);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<RuleDefinition>> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(service).count(wrapperCaptor.capture());
        LambdaQueryWrapper<RuleDefinition> wrapper = wrapperCaptor.getValue();
        wrapper.getSqlSegment();
        assertTrue(wrapper.getParamNameValuePairs().values().contains(0));
        verify(service).save(rule);
    }

    @Test
    void previewEffectiveRuleShouldPreferBindingScopeOverOtherEnabledCandidates() {
        RuleDefinitionServiceImpl service = spy(new RuleDefinitionServiceImpl(riskMetricCatalogService));
        RuleDefinition metricRule = buildEnabledRule(1L, "METRIC", "value", "value >= 1");
        RuleDefinition productRule = buildEnabledRule(2L, "PRODUCT", "value", "value >= 2");
        productRule.setProductId(1001L);
        RuleDefinition bindingRule = buildEnabledRule(3L, "BINDING", "value", "value >= 3");
        bindingRule.setRiskPointDeviceId(9001L);
        doReturn(java.util.List.of(metricRule, productRule, bindingRule))
                .when(service).list(any(LambdaQueryWrapper.class));

        RuleDefinitionEffectivePreviewVO preview = service.previewEffectiveRule(
                1L,
                null,
                "value",
                1001L,
                "MONITORING",
                8001L,
                9001L
        );

        assertEquals(Boolean.TRUE, preview.getHasMatchedRule());
        assertEquals(3L, preview.getMatchedRule().getId());
        assertEquals("BINDING", preview.getMatchedScope());
        assertEquals(3, preview.getCandidates().size());
        assertTrue(preview.getCandidates().stream()
                .anyMatch(item -> Long.valueOf(3L).equals(item.getRuleId()) && Boolean.TRUE.equals(item.getSelected())));
    }

    @Test
    void previewEffectiveRuleShouldSelectProductDefaultWhenNoDeviceOrBindingContextMatches() {
        RuleDefinitionServiceImpl service = spy(new RuleDefinitionServiceImpl(riskMetricCatalogService));
        RuleDefinition metricRule = buildEnabledRule(1L, "METRIC", "value", "value >= 1");
        RuleDefinition productTypeRule = buildEnabledRule(2L, "PRODUCT_TYPE", "value", "value >= 2");
        productTypeRule.setProductType("MONITORING");
        RuleDefinition productRule = buildEnabledRule(3L, "PRODUCT", "value", "value >= 3");
        productRule.setProductId(1001L);
        RuleDefinition deviceRule = buildEnabledRule(4L, "DEVICE", "value", "value >= 4");
        deviceRule.setDeviceId(8001L);
        doReturn(java.util.List.of(metricRule, productTypeRule, productRule, deviceRule))
                .when(service).list(any(LambdaQueryWrapper.class));

        RuleDefinitionEffectivePreviewVO preview = service.previewEffectiveRule(
                1L,
                null,
                "value",
                1001L,
                "monitoring",
                null,
                null
        );

        assertEquals(Boolean.TRUE, preview.getHasMatchedRule());
        assertEquals(3L, preview.getMatchedRule().getId());
        assertEquals("PRODUCT", preview.getMatchedScope());
        assertTrue(preview.getCandidates().stream()
                .anyMatch(item -> Long.valueOf(4L).equals(item.getRuleId())
                        && Boolean.FALSE.equals(item.getMatchedContext())));
    }

    private RuleDefinition buildEnabledRule(Long id, String scope, String metricIdentifier, String expression) {
        RuleDefinition rule = new RuleDefinition();
        rule.setId(id);
        rule.setRuleName(scope + " threshold");
        rule.setRuleScope(scope);
        rule.setMetricIdentifier(metricIdentifier);
        rule.setMetricName(metricIdentifier);
        rule.setAlarmLevel("orange");
        rule.setExpression(expression);
        rule.setStatus(0);
        rule.setDeleted(0);
        return rule;
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

        service.pageRuleList(null, "value", null, null, "product", 1001L, null, 1L, 10L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<RuleDefinition>> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(service).page(any(Page.class), wrapperCaptor.capture());
        LambdaQueryWrapper<RuleDefinition> wrapper = wrapperCaptor.getValue();
        wrapper.getSqlSegment();
        assertTrue(wrapper.getParamNameValuePairs().values().contains("value"));
        assertTrue(wrapper.getParamNameValuePairs().values().contains("PRODUCT"));
        assertTrue(wrapper.getParamNameValuePairs().values().contains(1001L));
    }

    @Test
    void pageRuleListShouldFilterByProductTypeTemplate() {
        initLambdaCache();
        RuleDefinitionServiceImpl service = spy(new RuleDefinitionServiceImpl(riskMetricCatalogService));
        Page<RuleDefinition> page = new Page<>(1L, 10L);
        page.setRecords(java.util.List.of());
        page.setTotal(0L);
        doReturn(page).when(service).page(any(Page.class), any(LambdaQueryWrapper.class));

        service.pageRuleList(null, "value", null, null, "product_type", null, "monitoring", 1L, 10L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<RuleDefinition>> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(service).page(any(Page.class), wrapperCaptor.capture());
        LambdaQueryWrapper<RuleDefinition> wrapper = wrapperCaptor.getValue();
        wrapper.getSqlSegment();
        assertTrue(wrapper.getParamNameValuePairs().values().contains("value"));
        assertTrue(wrapper.getParamNameValuePairs().values().contains("PRODUCT_TYPE"));
        assertTrue(wrapper.getParamNameValuePairs().values().contains("MONITORING"));
    }

    @Test
    void pageRuleListShouldFilterBusinessScopeView() {
        initLambdaCache();
        RuleDefinitionServiceImpl service = spy(new RuleDefinitionServiceImpl(riskMetricCatalogService));
        Page<RuleDefinition> page = new Page<>(1L, 10L);
        page.setRecords(java.util.List.of());
        page.setTotal(0L);
        doReturn(page).when(service).page(any(Page.class), any(LambdaQueryWrapper.class));

        service.pageRuleList(null, null, null, null, null, "business", null, null, 1L, 10L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<RuleDefinition>> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(service).page(any(Page.class), wrapperCaptor.capture());
        LambdaQueryWrapper<RuleDefinition> wrapper = wrapperCaptor.getValue();
        wrapper.getSqlSegment();
        assertTrue(wrapper.getParamNameValuePairs().values().contains("PRODUCT"));
        assertTrue(wrapper.getParamNameValuePairs().values().contains("DEVICE"));
        assertTrue(wrapper.getParamNameValuePairs().values().contains("BINDING"));
    }

    @Test
    void pageRuleListShouldFilterSystemScopeView() {
        initLambdaCache();
        RuleDefinitionServiceImpl service = spy(new RuleDefinitionServiceImpl(riskMetricCatalogService));
        Page<RuleDefinition> page = new Page<>(1L, 10L);
        page.setRecords(java.util.List.of());
        page.setTotal(0L);
        doReturn(page).when(service).page(any(Page.class), any(LambdaQueryWrapper.class));

        service.pageRuleList(null, null, null, null, null, "system", null, null, 1L, 10L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<RuleDefinition>> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(service).page(any(Page.class), wrapperCaptor.capture());
        LambdaQueryWrapper<RuleDefinition> wrapper = wrapperCaptor.getValue();
        wrapper.getSqlSegment();
        assertTrue(wrapper.getParamNameValuePairs().values().contains("METRIC"));
        assertTrue(wrapper.getParamNameValuePairs().values().contains("PRODUCT_TYPE"));
    }
}
