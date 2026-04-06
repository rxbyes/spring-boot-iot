package com.ghlzm.iot.alarm.service;

import com.ghlzm.iot.alarm.entity.RiskMetricCatalog;
import com.ghlzm.iot.device.entity.ProductModel;
import java.util.List;
import java.util.Set;

/**
 * 风险指标目录服务。
 */
public interface RiskMetricCatalogService {

    void publishFromReleasedContracts(Long productId, List<ProductModel> releasedContracts, Set<String> riskEnabledIdentifiers);

    RiskMetricCatalog getRequiredByProductAndIdentifier(Long productId, String contractIdentifier);

    RiskMetricCatalog getByProductAndIdentifier(Long productId, String contractIdentifier);

    RiskMetricCatalog getById(Long riskMetricId);

    List<RiskMetricCatalog> listEnabledByProduct(Long productId);
}
