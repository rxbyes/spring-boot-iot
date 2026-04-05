package com.ghlzm.iot.protocol.core.model;

import lombok.Data;

import java.util.List;

/**
 * 协议模板治理/诊断证据。
 */
@Data
public class ProtocolTemplateEvidence {

    private List<String> templateCodes = List.of();
    private List<ProtocolTemplateExecutionEvidence> executions = List.of();
}
