package com.ghlzm.iot.admin.observability.alerting;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 规则化运维告警调度器。
 */
@Component
@ConditionalOnProperty(value = "iot.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class ObservabilityAlertingScheduler {

    private final ObservabilityAlertingService observabilityAlertingService;

    public ObservabilityAlertingScheduler(ObservabilityAlertingService observabilityAlertingService) {
        this.observabilityAlertingService = observabilityAlertingService;
    }

    @Scheduled(
            initialDelayString = "#{T(java.lang.Math).max(@iotProperties.observability.alerting.evaluateIntervalSeconds, 60) * 1000L}",
            fixedDelayString = "#{T(java.lang.Math).max(@iotProperties.observability.alerting.evaluateIntervalSeconds, 60) * 1000L}"
    )
    public void evaluateAlerts() {
        observabilityAlertingService.evaluateAlerts();
    }
}
