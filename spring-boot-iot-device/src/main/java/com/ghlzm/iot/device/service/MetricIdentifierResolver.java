package com.ghlzm.iot.device.service;

import com.ghlzm.iot.device.service.model.MetricIdentifierResolution;
import com.ghlzm.iot.device.service.model.PublishedProductContractSnapshot;

/**
 * 指标标识符解析器。
 */
public interface MetricIdentifierResolver {

    MetricIdentifierResolution resolveForRead(PublishedProductContractSnapshot snapshot, String metricIdentifier);

    MetricIdentifierResolution resolveForRuntime(PublishedProductContractSnapshot snapshot, String metricIdentifier);

    MetricIdentifierResolution resolveForGovernance(PublishedProductContractSnapshot snapshot, String metricIdentifier);
}
