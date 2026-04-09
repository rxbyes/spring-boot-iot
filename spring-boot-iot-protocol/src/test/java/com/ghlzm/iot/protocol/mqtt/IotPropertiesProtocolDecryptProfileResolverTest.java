package com.ghlzm.iot.protocol.mqtt;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.framework.config.IotProperties;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IotPropertiesProtocolDecryptProfileResolverTest {

    @Test
    void shouldPreferFamilyBoundProfileOverAppIdFallback() {
        IotProperties properties = new IotProperties();
        properties.getProtocol().getDecryptProfiles().put("aes-62000000",
                decryptProfile("aes-62000000", "AES", "SPRING_CLOUD_AES", "62000000"));
        properties.getProtocol().getDecryptProfiles().put("des-62000001",
                decryptProfile("des-62000001", "DES", "IOT_PROTOCOL_CRYPTO", "62000001"));
        properties.getProtocol().getFamilyDefinitions().put("legacy-dp-crack",
                familyDefinition("legacy-dp-crack", "mqtt-json", "des-62000001"));

        IotPropertiesProtocolDecryptProfileResolver resolver =
                new IotPropertiesProtocolDecryptProfileResolver(properties);

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
        IotProperties properties = new IotProperties();
        properties.getProtocol().getDecryptProfiles().put("aes-62000000",
                decryptProfile("aes-62000000", "AES", "SPRING_CLOUD_AES", "62000000"));

        IotPropertiesProtocolDecryptProfileResolver resolver =
                new IotPropertiesProtocolDecryptProfileResolver(properties);

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
        IotProperties properties = new IotProperties();
        IotProperties.Protocol.DecryptProfile profile =
                decryptProfile("des-62000001", "DES", "IOT_PROTOCOL_CRYPTO", "62000001");
        profile.setEnabled(Boolean.FALSE);
        properties.getProtocol().getDecryptProfiles().put("des-62000001", profile);
        properties.getProtocol().getFamilyDefinitions().put("legacy-dp-gnss",
                familyDefinition("legacy-dp-gnss", "mqtt-json", "des-62000001"));

        IotPropertiesProtocolDecryptProfileResolver resolver =
                new IotPropertiesProtocolDecryptProfileResolver(properties);

        BizException ex = assertThrows(BizException.class, () -> resolver.resolveOrThrow(
                new ProtocolDecryptResolveContext("62000001", "mqtt-json", List.of("legacy-dp-gnss"))
        ));

        assertTrue(ex.getMessage().contains("decrypt profile 未启用"));
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
