package com.ghlzm.iot.alarm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ghlzm.iot.alarm.entity.RiskMetricCatalog;
import com.ghlzm.iot.alarm.mapper.RiskMetricCatalogMapper;
import com.ghlzm.iot.alarm.service.RiskMetricCatalogService;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.device.entity.ProductModel;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 风险指标目录服务实现。
 */
@Service
public class RiskMetricCatalogServiceImpl implements RiskMetricCatalogService {

    private final RiskMetricCatalogMapper riskMetricCatalogMapper;

    public RiskMetricCatalogServiceImpl(RiskMetricCatalogMapper riskMetricCatalogMapper) {
        this.riskMetricCatalogMapper = riskMetricCatalogMapper;
    }

    @Override
    public void publishFromReleasedContracts(Long productId,
                                             List<ProductModel> releasedContracts,
                                             Set<String> riskEnabledIdentifiers) {
        if (productId == null || releasedContracts == null || releasedContracts.isEmpty()) {
            return;
        }
        Set<String> enabledIdentifiers = normalizeIdentifiers(riskEnabledIdentifiers);
        if (enabledIdentifiers.isEmpty()) {
            return;
        }
        Map<String, RiskMetricCatalog> existingByIdentifier = listAllByProduct(productId).stream()
                .filter(row -> StringUtils.hasText(row.getContractIdentifier()))
                .collect(LinkedHashMap::new, (map, row) -> map.put(row.getContractIdentifier(), row), Map::putAll);
        Set<String> publishedIdentifiers = new LinkedHashSet<>();
        for (ProductModel contract : releasedContracts) {
            String identifier = normalize(contract == null ? null : contract.getIdentifier());
            if (!StringUtils.hasText(identifier) || !enabledIdentifiers.contains(identifier)) {
                continue;
            }
            RiskMetricCatalog row = existingByIdentifier.remove(identifier);
            if (row == null) {
                row = new RiskMetricCatalog();
                row.setProductId(productId);
                row.setContractIdentifier(identifier);
                row.setEnabled(1);
                populateCatalogRow(row, contract);
                riskMetricCatalogMapper.insert(row);
            } else {
                populateCatalogRow(row, contract);
                row.setEnabled(1);
                row.setDeleted(0);
                riskMetricCatalogMapper.updateById(row);
            }
            publishedIdentifiers.add(identifier);
        }

        for (RiskMetricCatalog stale : existingByIdentifier.values()) {
            if (stale == null || !StringUtils.hasText(stale.getContractIdentifier())) {
                continue;
            }
            if (publishedIdentifiers.contains(stale.getContractIdentifier())) {
                continue;
            }
            stale.setEnabled(0);
            riskMetricCatalogMapper.updateById(stale);
        }
    }

    @Override
    public RiskMetricCatalog getRequiredByProductAndIdentifier(Long productId, String contractIdentifier) {
        RiskMetricCatalog catalog = getByProductAndIdentifier(productId, contractIdentifier);
        if (catalog == null) {
            throw new BizException("风险指标目录未发布: " + contractIdentifier);
        }
        return catalog;
    }

    @Override
    public RiskMetricCatalog getByProductAndIdentifier(Long productId, String contractIdentifier) {
        String normalizedIdentifier = normalize(contractIdentifier);
        if (productId == null || !StringUtils.hasText(normalizedIdentifier)) {
            return null;
        }
        return riskMetricCatalogMapper.selectOne(new LambdaQueryWrapper<RiskMetricCatalog>()
                .eq(RiskMetricCatalog::getDeleted, 0)
                .eq(RiskMetricCatalog::getEnabled, 1)
                .eq(RiskMetricCatalog::getProductId, productId)
                .eq(RiskMetricCatalog::getContractIdentifier, normalizedIdentifier)
                .last("limit 1"));
    }

    @Override
    public RiskMetricCatalog getById(Long riskMetricId) {
        if (riskMetricId == null) {
            return null;
        }
        RiskMetricCatalog catalog = riskMetricCatalogMapper.selectById(riskMetricId);
        if (catalog == null || Integer.valueOf(1).equals(catalog.getDeleted()) || !Integer.valueOf(1).equals(catalog.getEnabled())) {
            return null;
        }
        return catalog;
    }

    @Override
    public List<RiskMetricCatalog> listEnabledByProduct(Long productId) {
        if (productId == null) {
            return List.of();
        }
        List<RiskMetricCatalog> rows = riskMetricCatalogMapper.selectList(new LambdaQueryWrapper<RiskMetricCatalog>()
                .eq(RiskMetricCatalog::getDeleted, 0)
                .eq(RiskMetricCatalog::getEnabled, 1)
                .eq(RiskMetricCatalog::getProductId, productId));
        return rows == null ? List.of() : rows;
    }

    private List<RiskMetricCatalog> listAllByProduct(Long productId) {
        if (productId == null) {
            return List.of();
        }
        List<RiskMetricCatalog> rows = riskMetricCatalogMapper.selectList(new LambdaQueryWrapper<RiskMetricCatalog>()
                .eq(RiskMetricCatalog::getProductId, productId));
        return rows == null ? List.of() : rows;
    }

    private void populateCatalogRow(RiskMetricCatalog row, ProductModel contract) {
        String identifier = normalize(contract == null ? null : contract.getIdentifier());
        row.setProductModelId(contract == null ? null : contract.getId());
        row.setContractIdentifier(identifier);
        row.setRiskMetricCode(buildRiskMetricCode(row.getProductId(), identifier));
        row.setRiskMetricName(resolveRiskMetricName(contract));
        row.setThresholdDirection(resolveThresholdDirection(identifier));
        row.setTrendEnabled(1);
        row.setGisEnabled(1);
        row.setInsightEnabled(1);
        row.setAnalyticsEnabled(1);
    }

    private String resolveRiskMetricName(ProductModel contract) {
        String modelName = contract == null ? null : normalize(contract.getModelName());
        if (StringUtils.hasText(modelName)) {
            return modelName;
        }
        return normalize(contract == null ? null : contract.getIdentifier());
    }

    private String buildRiskMetricCode(Long productId, String identifier) {
        String normalizedIdentifier = normalize(identifier);
        if (!StringUtils.hasText(normalizedIdentifier)) {
            return null;
        }
        return "RM_" + (productId == null ? "GLOBAL" : productId) + "_" + normalizedIdentifier.toUpperCase(Locale.ROOT);
    }

    private String resolveThresholdDirection(String identifier) {
        return "value".equalsIgnoreCase(normalize(identifier)) ? "HIGHER_IS_RISKIER" : null;
    }

    private Set<String> normalizeIdentifiers(Set<String> identifiers) {
        if (identifiers == null || identifiers.isEmpty()) {
            return Set.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String identifier : identifiers) {
            String value = normalize(identifier);
            if (StringUtils.hasText(value)) {
                normalized.add(value);
            }
        }
        return normalized;
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
