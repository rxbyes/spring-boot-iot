package com.ghlzm.iot.protocol.mqtt;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.framework.config.IotProperties;
import com.ghlzm.iot.framework.protocol.ProtocolSecurityDefinitionProvider;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IotPropertiesProtocolDecryptProfileResolverTest {

    @Test
    void shouldPreferFamilyBoundProfileOverAppIdFallback() {
        ProtocolSecurityDefinitionProvider provider = provider(
                List.of(familyDefinition("legacy-dp-crack", "mqtt-json", "des-62000001")),
                List.of(
                        decryptProfile("aes-62000000", "AES", "SPRING_CLOUD_AES", "62000000"),
                        decryptProfile("des-62000001", "DES", "IOT_PROTOCOL_CRYPTO", "62000001")
                )
        );

        IotPropertiesProtocolDecryptProfileResolver resolver =
                new IotPropertiesProtocolDecryptProfileResolver(provider);

        ProtocolDecryptProfile resolved = resolver.resolveOrThrow(new ProtocolDecryptResolveContext(
                "62000000",
                "mqtt-json",
                List.of("legacy-dp-crack")
        ));

        assertEquals("des-62000001", resolved.getProfileCode());
        assertEquals("DES", resolved.getAlgorithm());
        assertEquals("IOT_PROTOCOL_CRYPTO", resolved.getMerchantSource());
        assertEquals("62000001", resolved.getMerchantKey());
    }

    @Test
    void shouldFallbackToAppIdProfileWhenNoFamilyMatches() {
        ProtocolSecurityDefinitionProvider provider = provider(
                List.of(),
                List.of(decryptProfile("aes-62000000", "AES", "SPRING_CLOUD_AES", "62000000"))
        );

        IotPropertiesProtocolDecryptProfileResolver resolver =
                new IotPropertiesProtocolDecryptProfileResolver(provider);

        ProtocolDecryptProfile resolved = resolver.resolveOrThrow(new ProtocolDecryptResolveContext(
                "62000000",
                "mqtt-json",
                List.of()
        ));

        assertEquals("aes-62000000", resolved.getProfileCode());
        assertEquals("AES", resolved.getAlgorithm());
        assertEquals("SPRING_CLOUD_AES", resolved.getMerchantSource());
        assertEquals("62000000", resolved.getMerchantKey());
    }

    @Test
    void shouldRejectDisabledProfileBoundByFamily() {
        IotProperties.Protocol.DecryptProfile profile =
                decryptProfile("des-62000001", "DES", "IOT_PROTOCOL_CRYPTO", "62000001");
        profile.setEnabled(Boolean.FALSE);
        ProtocolSecurityDefinitionProvider provider = provider(
                List.of(familyDefinition("legacy-dp-gnss", "mqtt-json", "des-62000001")),
                List.of(profile)
        );

        IotPropertiesProtocolDecryptProfileResolver resolver =
                new IotPropertiesProtocolDecryptProfileResolver(provider);

        BizException ex = assertThrows(BizException.class, () -> resolver.resolveOrThrow(
                new ProtocolDecryptResolveContext("62000001", "mqtt-json", List.of("legacy-dp-gnss"))
        ));

        assertTrue(ex.getMessage().contains("decrypt profile 未启用"));
    }

    private ProtocolSecurityDefinitionProvider provider(List<IotProperties.Protocol.FamilyDefinition> families,
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

    private IotProperties.Protocol.FamilyDefinition familyDefinition(String familyCode,
                                                                     String protocolCode,
                                                                     String decryptProfileCode) {
        IotProperties.Protocol.FamilyDefinition definition = new IotProperties.Protocol.FamilyDefinition();
        definition.setFamilyCode(familyCode);
        definition.setProtocolCode(protocolCode);
        definition.setDecryptProfileCode(decryptProfileCode);
        return definition;
    }

    private IotProperties.Protocol.DecryptProfile decryptProfile(String profileCode,
                                                                 String algorithm,
                                                                 String merchantSource,
                                                                 String merchantKey) {
        IotProperties.Protocol.DecryptProfile profile = new IotProperties.Protocol.DecryptProfile();
        profile.setProfileCode(profileCode);
        profile.setAlgorithm(algorithm);
        profile.setMerchantSource(merchantSource);
        profile.setMerchantKey(merchantKey);
        return profile;
    }
}
