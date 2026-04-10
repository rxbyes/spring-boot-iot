package com.ghlzm.iot.alarm.service.impl;

import com.ghlzm.iot.alarm.entity.RiskMetricCatalog;
import com.ghlzm.iot.alarm.entity.RiskPointDevice;
import com.ghlzm.iot.alarm.entity.RuleDefinition;
import com.ghlzm.iot.alarm.service.RiskMetricCatalogService;
import com.ghlzm.iot.common.exception.BizException;
import java.util.Objects;
import org.springframework.util.StringUtils;

/**
 * 风险指标目录绑定共享校验。
 */
final class RiskMetricCatalogBindingSupport {

    private static final String LIFECYCLE_ACTIVE = "ACTIVE";

    private RiskMetricCatalogBindingSupport() {
    }

    static RiskMetricCatalog resolveCatalog(RiskMetricCatalogService riskMetricCatalogService,
                                            Long productId,
                                            Long riskMetricId,
                                            String metricIdentifier) {
        if (riskMetricCatalogService == null) {
            return null;
        }
        if (riskMetricId != null) {
            RiskMetricCatalog catalog = riskMetricCatalogService.getById(riskMetricId);
            if (catalog == null) {
                throw new BizException("风险指标目录不存在或已停用: " + riskMetricId);
            }
            if (productId != null && !Objects.equals(productId, catalog.getProductId())) {
                throw new BizException("风险指标不存在或不属于当前设备产品");
            }
            validateBindableCatalog(catalog);
            validateIdentifierConsistency(metricIdentifier, catalog);
            return catalog;
        }
        if (productId == null) {
            return null;
        }
        RiskMetricCatalog catalog = riskMetricCatalogService.getByProductAndIdentifier(productId, metricIdentifier);
        if (catalog != null) {
            validateBindableCatalog(catalog);
        }
        return catalog;
    }

    static void bindRiskPointDevice(RiskPointDevice riskPointDevice, RiskMetricCatalog catalog) {
        String contractIdentifier = requireBindableContractIdentifier(catalog);
        riskPointDevice.setRiskMetricId(catalog.getId());
        riskPointDevice.setMetricIdentifier(contractIdentifier);
        riskPointDevice.setMetricName(resolveMetricName(catalog.getRiskMetricName(), contractIdentifier));
    }

    static void bindRuleDefinition(RuleDefinition rule, RiskMetricCatalog catalog) {
        String contractIdentifier = requireBindableContractIdentifier(catalog);
        String normalizedIdentifier = normalizeMetricIdentifier(rule.getMetricIdentifier());
        if (StringUtils.hasText(normalizedIdentifier) && !contractIdentifier.equals(normalizedIdentifier)) {
            throw new BizException("目录指标与测点标识符不一致");
        }
        rule.setMetricIdentifier(contractIdentifier);
        if (!StringUtils.hasText(rule.getMetricName()) && StringUtils.hasText(catalog.getRiskMetricName())) {
            rule.setMetricName(catalog.getRiskMetricName().trim());
        }
    }

    private static String requireBindableContractIdentifier(RiskMetricCatalog catalog) {
        validateBindableCatalog(catalog);
        String contractIdentifier = normalizeMetricIdentifier(catalog == null ? null : catalog.getContractIdentifier());
        if (!StringUtils.hasText(contractIdentifier)) {
            throw new BizException("风险指标目录缺少合同字段标识: " + (catalog == null ? null : catalog.getId()));
        }
        return contractIdentifier;
    }

    private static void validateIdentifierConsistency(String metricIdentifier, RiskMetricCatalog catalog) {
        String normalizedIdentifier = normalizeMetricIdentifier(metricIdentifier);
        if (!StringUtils.hasText(normalizedIdentifier)) {
            return;
        }
        String contractIdentifier = requireBindableContractIdentifier(catalog);
        if (!contractIdentifier.equals(normalizedIdentifier)) {
            throw new BizException("目录指标与测点标识符不一致");
        }
    }

    private static void validateBindableCatalog(RiskMetricCatalog catalog) {
        if (catalog == null) {
            return;
        }
        String lifecycleStatus = normalizeMetricIdentifier(catalog.getLifecycleStatus());
        if (StringUtils.hasText(lifecycleStatus) && !LIFECYCLE_ACTIVE.equalsIgnoreCase(lifecycleStatus)) {
            throw new BizException("风险指标目录当前不可绑定: " + catalog.getId());
        }
    }

    private static String normalizeMetricIdentifier(String metricIdentifier) {
        return StringUtils.hasText(metricIdentifier) ? metricIdentifier.trim() : null;
    }

    private static String resolveMetricName(String metricName, String metricIdentifier) {
        String normalizedMetricName = normalizeMetricIdentifier(metricName);
        return normalizedMetricName == null ? metricIdentifier : normalizedMetricName;
    }
}
