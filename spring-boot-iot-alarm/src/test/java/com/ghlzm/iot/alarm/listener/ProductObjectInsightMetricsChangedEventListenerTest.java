package com.ghlzm.iot.alarm.listener;

import com.ghlzm.iot.alarm.service.RiskMetricCatalogRebuildService;
import com.ghlzm.iot.common.event.governance.ProductObjectInsightMetricsChangedEvent;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class ProductObjectInsightMetricsChangedEventListenerTest {

    @Test
    void onMetricsChangedShouldRebuildLatestCatalogForProduct() {
        RiskMetricCatalogRebuildService rebuildService = mock(RiskMetricCatalogRebuildService.class);
        ProductObjectInsightMetricsChangedEventListener listener =
                new ProductObjectInsightMetricsChangedEventListener(rebuildService);

        listener.onMetricsChanged(new ProductObjectInsightMetricsChangedEvent(1L, 1001L, 7001L, 9001L));

        verify(rebuildService).rebuildLatestRelease(1001L);
    }

    @Test
    void onMetricsChangedShouldRebuildCatalogEvenWhenReleaseBatchIsMissing() {
        RiskMetricCatalogRebuildService rebuildService = mock(RiskMetricCatalogRebuildService.class);
        ProductObjectInsightMetricsChangedEventListener listener =
                new ProductObjectInsightMetricsChangedEventListener(rebuildService);

        listener.onMetricsChanged(new ProductObjectInsightMetricsChangedEvent(1L, 1001L, null, 9001L));

        verify(rebuildService).rebuildLatestRelease(1001L);
    }

    @Test
    void onMetricsChangedShouldIgnoreMissingProductId() {
        RiskMetricCatalogRebuildService rebuildService = mock(RiskMetricCatalogRebuildService.class);
        ProductObjectInsightMetricsChangedEventListener listener =
                new ProductObjectInsightMetricsChangedEventListener(rebuildService);

        listener.onMetricsChanged(new ProductObjectInsightMetricsChangedEvent(1L, null, 7001L, 9001L));

        verify(rebuildService, never()).rebuildLatestRelease(anyLong());
    }
}
