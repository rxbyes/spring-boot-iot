package com.ghlzm.iot.framework.protocol.template;

import java.util.List;

@FunctionalInterface
public interface ProtocolTemplateDefinitionProvider {

    List<ProtocolTemplateRuntimeDefinition> listPublishedDefinitions();
}
