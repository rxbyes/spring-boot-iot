package com.ghlzm.iot.protocol.mqtt.legacy.template.runtime;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

public record CompiledLegacyDpTemplate(String templateCode,
                                       String familyCode,
                                       String protocolCode,
                                       Pattern logicalPattern,
                                       boolean objectPayload,
                                       Map<String, String> outputMappings) {

    public CompiledLegacyDpTemplate {
        outputMappings = outputMappings == null || outputMappings.isEmpty()
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(outputMappings));
    }
}
