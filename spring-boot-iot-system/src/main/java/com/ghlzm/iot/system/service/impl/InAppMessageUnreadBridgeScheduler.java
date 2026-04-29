package com.ghlzm.iot.system.service.impl;

import com.ghlzm.iot.system.service.InAppMessageUnreadBridgeService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 站内消息未读桥接调度器。
 */
@Component
@ConditionalOnProperty(value = "iot.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class InAppMessageUnreadBridgeScheduler {

    private final InAppMessageUnreadBridgeService inAppMessageUnreadBridgeService;

    public InAppMessageUnreadBridgeScheduler(InAppMessageUnreadBridgeService inAppMessageUnreadBridgeService) {
        this.inAppMessageUnreadBridgeService = inAppMessageUnreadBridgeService;
    }

    @Scheduled(
            initialDelayString = "#{T(java.lang.Math).max(@iotProperties.observability.inAppUnreadBridge.scanIntervalSeconds, 15) * 1000L}",
            fixedDelayString = "#{T(java.lang.Math).max(@iotProperties.observability.inAppUnreadBridge.scanIntervalSeconds, 15) * 1000L}"
    )
    public void scanUnreadMessages() {
        inAppMessageUnreadBridgeService.scanAndBridgeUnreadMessages();
    }
}
