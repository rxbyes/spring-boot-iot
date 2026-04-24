package com.ghlzm.iot.message.service.capability;

import com.ghlzm.iot.device.service.CommandRecordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class CapabilityFeedbackHandler {

    private static final Logger log = LoggerFactory.getLogger(CapabilityFeedbackHandler.class);

    private final CapabilityFeedbackParser parser;
    private final CommandRecordService commandRecordService;

    public CapabilityFeedbackHandler(CapabilityFeedbackParser parser, CommandRecordService commandRecordService) {
        this.parser = parser;
        this.commandRecordService = commandRecordService;
    }

    public CapabilityFeedback handle(String topic, String rawPayload) {
        CapabilityFeedback feedback = parser.parse(rawPayload);
        if (!feedback.valid()) {
            log.warn("设备能力反馈解析失败, topic={}, reason={}, rawPayload={}", topic, feedback.invalidReason(), rawPayload);
            return feedback;
        }

        LocalDateTime now = LocalDateTime.now();
        if ("sucd".equalsIgnoreCase(feedback.result())) {
            boolean updated = commandRecordService.markSuccessByCommandId(feedback.msgid(), feedback.rawPayload(), now);
            if (!updated) {
                log.warn("设备能力反馈未找到命令记录, topic={}, msgid={}, rawPayload={}", topic, feedback.msgid(), rawPayload);
            }
            return feedback;
        }
        if ("fail".equalsIgnoreCase(feedback.result())) {
            boolean updated = commandRecordService.markFailedByCommandId(feedback.msgid(), feedback.rawPayload(), feedback.message(), now);
            if (!updated) {
                log.warn("设备能力反馈未找到命令记录, topic={}, msgid={}, rawPayload={}", topic, feedback.msgid(), rawPayload);
            }
            return feedback;
        }
        log.warn("设备能力反馈 result 不支持, topic={}, msgid={}, result={}, rawPayload={}", topic, feedback.msgid(), feedback.result(), rawPayload);
        return feedback;
    }
}
