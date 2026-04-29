package com.ghlzm.iot.alarm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ghlzm.iot.alarm.entity.RiskPointDevice;
import com.ghlzm.iot.alarm.entity.RuleDefinition;
import com.ghlzm.iot.alarm.mapper.RiskPointDeviceMapper;
import com.ghlzm.iot.alarm.service.DeviceThresholdReadService;
import com.ghlzm.iot.alarm.service.RiskPointBindingMaintenanceService;
import com.ghlzm.iot.alarm.service.RuleDefinitionService;
import com.ghlzm.iot.alarm.vo.DeviceThresholdOverviewVO;
import com.ghlzm.iot.alarm.vo.RuleDefinitionEffectivePreviewVO;
import com.ghlzm.iot.common.device.DeviceBindingCapabilitySupport;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.service.DeviceService;
import com.ghlzm.iot.device.service.ProductService;
import com.ghlzm.iot.device.vo.DeviceMetricOptionVO;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
public class DeviceThresholdReadServiceImpl implements DeviceThresholdReadService {

    private final DeviceService deviceService;
    private final ProductService productService;
    private final RiskPointBindingMaintenanceService bindingMaintenanceService;
    private final RiskPointDeviceMapper riskPointDeviceMapper;
    private final RuleDefinitionService ruleDefinitionService;

    public DeviceThresholdReadServiceImpl(DeviceService deviceService,
                                          ProductService productService,
                                          RiskPointBindingMaintenanceService bindingMaintenanceService,
                                          RiskPointDeviceMapper riskPointDeviceMapper,
                                          RuleDefinitionService ruleDefinitionService) {
        this.deviceService = deviceService;
        this.productService = productService;
        this.bindingMaintenanceService = bindingMaintenanceService;
        this.riskPointDeviceMapper = riskPointDeviceMapper;
        this.ruleDefinitionService = ruleDefinitionService;
    }

    @Override
    public DeviceThresholdOverviewVO getDeviceThresholds(Long currentUserId, Long deviceId) {
        Device device = deviceService.getRequiredById(currentUserId, deviceId);
        Product product = productService.getRequiredById(device.getProductId());
        String productType = DeviceBindingCapabilitySupport.resolve(product.getProductKey(), product.getProductName()).name();

        DeviceThresholdOverviewVO overview = new DeviceThresholdOverviewVO();
        overview.setDeviceId(device.getId());
        overview.setDeviceCode(device.getDeviceCode());
        overview.setDeviceName(device.getDeviceName());
        overview.setProductId(product.getId());
        overview.setProductName(product.getProductName());

        List<DeviceMetricOptionVO> formalMetrics = bindingMaintenanceService.listFormalBindingMetricOptions(deviceId, currentUserId);
        List<RiskPointDevice> bindings = riskPointDeviceMapper.selectList(new LambdaQueryWrapper<RiskPointDevice>()
                .eq(RiskPointDevice::getDeleted, 0)
                .eq(RiskPointDevice::getDeviceId, deviceId));

        LinkedHashMap<String, DeviceThresholdOverviewVO.MetricItem> itemMap = new LinkedHashMap<>();
        LinkedHashMap<String, List<RiskPointDevice>> bindingMap = new LinkedHashMap<>();
        mergeFormalMetrics(itemMap, formalMetrics);
        mergeBindings(itemMap, bindingMap, bindings);

        int matchedMetricCount = 0;
        int missingMetricCount = 0;
        for (Map.Entry<String, DeviceThresholdOverviewVO.MetricItem> entry : itemMap.entrySet()) {
            DeviceThresholdOverviewVO.MetricItem item = entry.getValue();
            RuleDefinitionEffectivePreviewVO basePreview = ruleDefinitionService.previewEffectiveRule(
                    device.getTenantId(),
                    item.getRiskMetricId(),
                    item.getMetricIdentifier(),
                    product.getId(),
                    productType,
                    device.getId(),
                    null
            );
            appendGroupedRules(item, basePreview);

            List<RiskPointDevice> metricBindings = bindingMap.getOrDefault(entry.getKey(), List.of());
            if (metricBindings.isEmpty()) {
                appendMatchedRule(item.getEffectiveRules(), basePreview);
            } else {
                for (RiskPointDevice binding : metricBindings) {
                    RuleDefinitionEffectivePreviewVO bindingPreview = ruleDefinitionService.previewEffectiveRule(
                            device.getTenantId(),
                            item.getRiskMetricId(),
                            item.getMetricIdentifier(),
                            product.getId(),
                            productType,
                            device.getId(),
                            binding.getId()
                    );
                    appendGroupedRules(item, bindingPreview);
                    appendMatchedRule(item.getEffectiveRules(), bindingPreview);
                }
            }

            if (CollectionUtils.isEmpty(item.getEffectiveRules())) {
                missingMetricCount++;
            } else {
                matchedMetricCount++;
            }
            overview.getItems().add(item);
        }
        overview.setMatchedMetricCount(matchedMetricCount);
        overview.setMissingMetricCount(missingMetricCount);
        return overview;
    }

    private void mergeFormalMetrics(Map<String, DeviceThresholdOverviewVO.MetricItem> itemMap,
                                    List<DeviceMetricOptionVO> formalMetrics) {
        if (CollectionUtils.isEmpty(formalMetrics)) {
            return;
        }
        for (DeviceMetricOptionVO metric : formalMetrics) {
            if (metric == null || !StringUtils.hasText(metric.getIdentifier())) {
                continue;
            }
            DeviceThresholdOverviewVO.MetricItem item = itemMap.computeIfAbsent(metricKey(metric.getRiskMetricId(), metric.getIdentifier()),
                    ignored -> newMetricItem(metric.getRiskMetricId(), metric.getIdentifier(), metric.getName()));
            if (!StringUtils.hasText(item.getMetricName()) && StringUtils.hasText(metric.getName())) {
                item.setMetricName(metric.getName().trim());
            }
        }
    }

    private void mergeBindings(Map<String, DeviceThresholdOverviewVO.MetricItem> itemMap,
                               Map<String, List<RiskPointDevice>> bindingMap,
                               List<RiskPointDevice> bindings) {
        if (CollectionUtils.isEmpty(bindings)) {
            return;
        }
        for (RiskPointDevice binding : bindings) {
            if (binding == null || !StringUtils.hasText(binding.getMetricIdentifier())) {
                continue;
            }
            String key = metricKey(binding.getRiskMetricId(), binding.getMetricIdentifier());
            DeviceThresholdOverviewVO.MetricItem item = itemMap.computeIfAbsent(key,
                    ignored -> newMetricItem(binding.getRiskMetricId(), binding.getMetricIdentifier(), binding.getMetricName()));
            if (!StringUtils.hasText(item.getMetricName()) && StringUtils.hasText(binding.getMetricName())) {
                item.setMetricName(binding.getMetricName().trim());
            }
            bindingMap.computeIfAbsent(key, ignored -> new java.util.ArrayList<>()).add(binding);
        }
    }

    private DeviceThresholdOverviewVO.MetricItem newMetricItem(Long riskMetricId, String metricIdentifier, String metricName) {
        DeviceThresholdOverviewVO.MetricItem item = new DeviceThresholdOverviewVO.MetricItem();
        item.setRiskMetricId(riskMetricId);
        item.setMetricIdentifier(metricIdentifier.trim());
        item.setMetricName(StringUtils.hasText(metricName) ? metricName.trim() : metricIdentifier.trim());
        return item;
    }

    private void appendGroupedRules(DeviceThresholdOverviewVO.MetricItem item, RuleDefinitionEffectivePreviewVO preview) {
        if (preview == null || CollectionUtils.isEmpty(preview.getCandidates())) {
            return;
        }
        for (RuleDefinitionEffectivePreviewVO.Candidate candidate : preview.getCandidates()) {
            if (candidate == null || Boolean.FALSE.equals(candidate.getMatchedContext())) {
                continue;
            }
            DeviceThresholdOverviewVO.RuleItem ruleItem = toRuleItem(candidate);
            switch (normalizeScope(candidate.getRuleScope())) {
                case "BINDING" -> appendUnique(item.getBindingRules(), ruleItem);
                case "DEVICE" -> appendUnique(item.getDeviceRules(), ruleItem);
                case "PRODUCT" -> appendUnique(item.getProductRules(), ruleItem);
                case "PRODUCT_TYPE", "METRIC" -> appendUnique(item.getFallbackRules(), ruleItem);
                default -> appendUnique(item.getFallbackRules(), ruleItem);
            }
        }
    }

    private void appendMatchedRule(List<DeviceThresholdOverviewVO.RuleItem> target, RuleDefinitionEffectivePreviewVO preview) {
        if (preview == null || !Boolean.TRUE.equals(preview.getHasMatchedRule()) || preview.getMatchedRule() == null) {
            return;
        }
        appendUnique(target, toRuleItem(preview));
    }

    private void appendUnique(List<DeviceThresholdOverviewVO.RuleItem> target, DeviceThresholdOverviewVO.RuleItem candidate) {
        if (candidate == null) {
            return;
        }
        boolean exists = target.stream().anyMatch(existing -> sameRule(existing, candidate));
        if (!exists) {
            target.add(candidate);
        }
    }

    private boolean sameRule(DeviceThresholdOverviewVO.RuleItem left, DeviceThresholdOverviewVO.RuleItem right) {
        return Objects.equals(left.getRuleId(), right.getRuleId())
                && Objects.equals(left.getRuleScope(), right.getRuleScope())
                && Objects.equals(left.getTargetLabel(), right.getTargetLabel())
                && Objects.equals(left.getRiskPointDeviceId(), right.getRiskPointDeviceId())
                && Objects.equals(left.getExpression(), right.getExpression());
    }

    private DeviceThresholdOverviewVO.RuleItem toRuleItem(RuleDefinitionEffectivePreviewVO preview) {
        RuleDefinition rule = preview.getMatchedRule();
        DeviceThresholdOverviewVO.RuleItem ruleItem = new DeviceThresholdOverviewVO.RuleItem();
        String scope = normalizeScope(rule.getRuleScope());
        String scopeText = StringUtils.hasText(preview.getMatchedScopeText())
                ? preview.getMatchedScopeText()
                : formatScopeText(scope);
        ruleItem.setRuleId(rule.getId());
        ruleItem.setRuleName(rule.getRuleName());
        ruleItem.setRuleScope(scope);
        ruleItem.setRuleScopeText(scopeText);
        ruleItem.setExpression(rule.getExpression());
        ruleItem.setAlarmLevel(rule.getAlarmLevel());
        ruleItem.setSourceLabel(scopeText);
        ruleItem.setTargetLabel(formatTargetLabel(rule));
        ruleItem.setRiskPointDeviceId(rule.getRiskPointDeviceId());
        return ruleItem;
    }

    private DeviceThresholdOverviewVO.RuleItem toRuleItem(RuleDefinitionEffectivePreviewVO.Candidate candidate) {
        DeviceThresholdOverviewVO.RuleItem ruleItem = new DeviceThresholdOverviewVO.RuleItem();
        String scope = normalizeScope(candidate.getRuleScope());
        String scopeText = StringUtils.hasText(candidate.getRuleScopeText())
                ? candidate.getRuleScopeText()
                : formatScopeText(scope);
        ruleItem.setRuleId(candidate.getRuleId());
        ruleItem.setRuleName(candidate.getRuleName());
        ruleItem.setRuleScope(scope);
        ruleItem.setRuleScopeText(scopeText);
        ruleItem.setExpression(candidate.getExpression());
        ruleItem.setAlarmLevel(candidate.getAlarmLevel());
        ruleItem.setSourceLabel(scopeText);
        ruleItem.setTargetLabel(candidate.getScopeTarget());
        ruleItem.setRiskPointDeviceId(extractBindingId(scope, candidate.getScopeTarget()));
        return ruleItem;
    }

    private Long extractBindingId(String scope, String scopeTarget) {
        if (!"BINDING".equals(scope) || !StringUtils.hasText(scopeTarget)) {
            return null;
        }
        String[] parts = scopeTarget.trim().split("\\s+");
        if (parts.length < 2) {
            return null;
        }
        try {
            return Long.parseLong(parts[1]);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String metricKey(Long riskMetricId, String metricIdentifier) {
        return (riskMetricId == null ? "null" : riskMetricId) + "::" + metricIdentifier.trim();
    }

    private String normalizeScope(String scope) {
        return StringUtils.hasText(scope) ? scope.trim().toUpperCase(Locale.ROOT) : "METRIC";
    }

    private String formatScopeText(String scope) {
        return switch (normalizeScope(scope)) {
            case "PRODUCT" -> "产品默认";
            case "DEVICE" -> "设备个性";
            case "BINDING" -> "绑定个性";
            case "PRODUCT_TYPE" -> "产品类型模板";
            default -> "测点通用";
        };
    }

    private String formatTargetLabel(RuleDefinition rule) {
        String scope = normalizeScope(rule.getRuleScope());
        return switch (scope) {
            case "BINDING" -> rule.getRiskPointDeviceId() == null ? "--" : "绑定 " + rule.getRiskPointDeviceId();
            case "DEVICE" -> rule.getDeviceId() == null ? "--" : "设备 " + rule.getDeviceId();
            case "PRODUCT" -> rule.getProductId() == null ? "--" : "产品 " + rule.getProductId();
            case "PRODUCT_TYPE" -> StringUtils.hasText(rule.getProductType()) ? rule.getProductType() : "--";
            default -> "通用";
        };
    }
}
