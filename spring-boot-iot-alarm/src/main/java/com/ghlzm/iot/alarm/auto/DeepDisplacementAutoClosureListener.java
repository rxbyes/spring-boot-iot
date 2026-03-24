package com.ghlzm.iot.alarm.auto;

import com.ghlzm.iot.device.event.DeviceRiskEvaluationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 设备属性入库后的自动闭环监听器。
 */
@Component
public class DeepDisplacementAutoClosureListener {

    private static final Logger log = LoggerFactory.getLogger(DeepDisplacementAutoClosureListener.class);

    private final DeepDisplacementAutoClosureService autoClosureService;

    public DeepDisplacementAutoClosureListener(DeepDisplacementAutoClosureService autoClosureService) {
        this.autoClosureService = autoClosureService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onDeviceRiskEvaluation(DeviceRiskEvaluationEvent event) {
        try {
            autoClosureService.process(event);
        } catch (Exception ex) {
            log.error("深部位移自动闭环执行失败, deviceCode={}, traceId={}",
                    event == null ? null : event.getDeviceCode(),
                    event == null ? null : event.getTraceId(),
                    ex);
        }
    }
}
