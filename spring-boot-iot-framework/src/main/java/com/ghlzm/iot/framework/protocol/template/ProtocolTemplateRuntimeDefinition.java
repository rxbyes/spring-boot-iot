package com.ghlzm.iot.framework.protocol.template;

public record ProtocolTemplateRuntimeDefinition(String templateCode,
                                                String familyCode,
                                                String protocolCode,
                                                String displayName,
                                                String expressionJson,
                                                String outputMappingJson,
                                                Integer publishedVersionNo) {
}
