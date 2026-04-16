package com.ghlzm.iot.framework.protocol;

import com.ghlzm.iot.framework.config.IotProperties;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CompositeProtocolSecurityDefinitionProviderTest {

    @Test
    void shouldPreferPublishedDefinitionsOverYamlFallback() {
        ProtocolSecurityDefinitionProvider published = stubProvider(
                List.of(family("legacy-dp-crack", "des-62000001")),
                List.of(profile("des-62000001", "DES"))
        );
        ProtocolSecurityDefinitionProvider yaml = stubProvider(
                List.of(family("legacy-dp-crack", "aes-62000000")),
                List.of(profile("aes-62000000", "AES"))
        );

        CompositeProtocolSecurityDefinitionProvider provider =
                new CompositeProtocolSecurityDefinitionProvider(List.of(published, yaml));

        assertEquals("des-62000001", provider.getFamilyDefinition("legacy-dp-crack").getDecryptProfileCode());
        assertEquals("DES", provider.getDecryptProfile("des-62000001").getAlgorithm());
    }

    private ProtocolSecurityDefinitionProvider stubProvider(List<IotProperties.Protocol.FamilyDefinition> families,
                                                            List<IotProperties.Protocol.DecryptProfile> profiles) {
        Map<String, IotProperties.Protocol.FamilyDefinition> familyMap = new LinkedHashMap<>();
        for (IotProperties.Protocol.FamilyDefinition family : families) {
            familyMap.put(family.getFamilyCode(), family);
        }
        Map<String, IotProperties.Protocol.DecryptProfile> profileMap = new LinkedHashMap<>();
        for (IotProperties.Protocol.DecryptProfile profile : profiles) {
            profileMap.put(profile.getProfileCode(), profile);
        }
        return new ProtocolSecurityDefinitionProvider() {
            @Override
            public IotProperties.Protocol.FamilyDefinition getFamilyDefinition(String familyCode) {
                return familyMap.get(familyCode);
            }

            @Override
            public IotProperties.Protocol.DecryptProfile getDecryptProfile(String profileCode) {
                return profileMap.get(profileCode);
            }

            @Override
            public Map<String, IotProperties.Protocol.FamilyDefinition> listFamilyDefinitions() {
                return familyMap;
            }

            @Override
            public Map<String, IotProperties.Protocol.DecryptProfile> listDecryptProfiles() {
                return profileMap;
            }
        };
    }

    private IotProperties.Protocol.FamilyDefinition family(String familyCode, String decryptProfileCode) {
        IotProperties.Protocol.FamilyDefinition definition = new IotProperties.Protocol.FamilyDefinition();
        definition.setFamilyCode(familyCode);
        definition.setProtocolCode("mqtt-json");
        definition.setDecryptProfileCode(decryptProfileCode);
        definition.setEnabled(Boolean.TRUE);
        return definition;
    }

    private IotProperties.Protocol.DecryptProfile profile(String profileCode, String algorithm) {
        IotProperties.Protocol.DecryptProfile profile = new IotProperties.Protocol.DecryptProfile();
        profile.setProfileCode(profileCode);
        profile.setAlgorithm(algorithm);
        profile.setMerchantSource("IOT_PROTOCOL_CRYPTO");
        profile.setMerchantKey(profileCode);
        profile.setEnabled(Boolean.TRUE);
        return profile;
    }
}
