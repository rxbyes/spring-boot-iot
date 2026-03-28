package com.ghlzm.iot.framework.observability.invalidreport;

import java.time.Duration;
import java.time.Instant;

/**
 * 无效上报治理计数与冷却存储。
 */
public interface InvalidReportCounterStore {

    long incrementFailureStage(String failureStage);

    long incrementReasonCode(String reasonCode);

    long sumFailureStageSince(String failureStage, Instant startInclusive);

    boolean tryOpenCooldown(String governanceKey, Duration ttl);

    void clearCooldown(String governanceKey);
}
