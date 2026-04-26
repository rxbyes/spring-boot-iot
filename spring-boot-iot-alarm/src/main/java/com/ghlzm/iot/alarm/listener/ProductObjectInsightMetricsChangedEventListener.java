package com.ghlzm.iot.alarm.listener;

import com.ghlzm.iot.alarm.service.RiskMetricCatalogRebuildService;
import com.ghlzm.iot.common.event.governance.ProductObjectInsightMetricsChangedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 契约字段页监测数据治理变更后，重建风险指标目录。
 */
@Component
public class ProductObjectInsightMetricsChangedEventListener {

    private final RiskMetricCatalogRebuildService rebuildService;

    public ProductObjectInsightMetricsChangedEventListener(RiskMetricCatalogRebuildService rebuildService) {
        this.rebuildService = rebuildService;
    }

    @EventListener
    public void onMetricsChanged(ProductObjectInsightMetricsChangedEvent event) {
        if (event == null || event.productId() == null || rebuildService == null) {
            return;
        }
        rebuildService.rebuildLatestRelease(event.productId());
    }
}
