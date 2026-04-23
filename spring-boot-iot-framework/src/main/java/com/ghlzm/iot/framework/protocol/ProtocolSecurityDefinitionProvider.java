package com.ghlzm.iot.framework.protocol;

import com.ghlzm.iot.framework.config.IotProperties;
import java.util.Map;

public interface ProtocolSecurityDefinitionProvider {

    IotProperties.Protocol.FamilyDefinition getFamilyDefinition(String familyCode);

    IotProperties.Protocol.DecryptProfile getDecryptProfile(String profileCode);

    Map<String, IotProperties.Protocol.FamilyDefinition> listFamilyDefinitions();

    Map<String, IotProperties.Protocol.DecryptProfile> listDecryptProfiles();
}
