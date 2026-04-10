package com.ghlzm.iot.alarm.listener;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ghlzm.iot.alarm.service.RiskMetricCatalogPublishRule;
import com.ghlzm.iot.alarm.service.RiskMetricCatalogService;
import com.ghlzm.iot.common.event.governance.ProductContractReleasedEvent;
import com.ghlzm.iot.device.entity.ProductModel;
import com.ghlzm.iot.device.mapper.ProductModelMapper;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 正式合同发布后同步生成风险指标目录。
 */
@Component
public class ProductContractReleasedEventListener {

    private final ProductModelMapper productModelMapper;
    private final RiskMetricCatalogPublishRule riskMetricCatalogPublishRule;
    private final RiskMetricCatalogService riskMetricCatalogService;

    public ProductContractReleasedEventListener(ProductModelMapper productModelMapper,
                                                RiskMetricCatalogPublishRule riskMetricCatalogPublishRule,
                                                RiskMetricCatalogService riskMetricCatalogService) {
        this.productModelMapper = productModelMapper;
        this.riskMetricCatalogPublishRule = riskMetricCatalogPublishRule;
        this.riskMetricCatalogService = riskMetricCatalogService;
    }

    @EventListener
    public void onProductContractReleased(ProductContractReleasedEvent event) {
        if (event == null || event.productId() == null || event.releaseBatchId() == null) {
            return;
        }
        Set<String> releasedIdentifiers = normalizeIdentifiers(event.releasedIdentifiers());
        if (releasedIdentifiers.isEmpty()) {
            return;
        }
        List<ProductModel> releasedContracts = productModelMapper.selectList(new LambdaQueryWrapper<ProductModel>()
                .eq(ProductModel::getDeleted, 0)
                .eq(ProductModel::getProductId, event.productId())
                .eq(ProductModel::getModelType, "property")
                .in(ProductModel::getIdentifier, releasedIdentifiers));
        if (releasedContracts == null || releasedContracts.isEmpty()) {
            return;
        }
        Set<String> riskEnabledIdentifiers = riskMetricCatalogPublishRule == null
                ? Set.of()
                : riskMetricCatalogPublishRule.resolveRiskEnabledIdentifiers(null, releasedContracts);
        riskMetricCatalogService.publishFromReleasedContracts(
                event.productId(),
                event.releaseBatchId(),
                releasedContracts,
                riskEnabledIdentifiers
        );
    }

    private Set<String> normalizeIdentifiers(List<String> identifiers) {
        if (identifiers == null || identifiers.isEmpty()) {
            return Set.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String identifier : identifiers) {
            if (!StringUtils.hasText(identifier)) {
                continue;
            }
            normalized.add(identifier.trim());
        }
        return normalized;
    }
}
