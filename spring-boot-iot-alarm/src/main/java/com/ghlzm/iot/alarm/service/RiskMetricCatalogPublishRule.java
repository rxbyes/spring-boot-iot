package com.ghlzm.iot.alarm.service;

import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.entity.ProductModel;

import java.util.List;
import java.util.Set;

/**
 * 风险指标目录发布规则。
 */
public interface RiskMetricCatalogPublishRule {

    Set<String> resolveRiskEnabledIdentifiers(Device device, List<ProductModel> releasedContracts);

    default Set<String> resolveRiskEnabledIdentifiers(Product product,
                                                      String scenarioCode,
                                                      Device device,
                                                      List<ProductModel> releasedContracts) {
        return resolveRiskEnabledIdentifiers(device, releasedContracts);
    }
}
