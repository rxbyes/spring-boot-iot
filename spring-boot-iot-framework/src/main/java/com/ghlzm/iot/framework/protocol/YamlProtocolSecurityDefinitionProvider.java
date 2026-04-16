package com.ghlzm.iot.framework.protocol;

import com.ghlzm.iot.framework.config.IotProperties;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component("yamlProtocolSecurityDefinitionProvider")
public class YamlProtocolSecurityDefinitionProvider implements ProtocolSecurityDefinitionProvider {

    private final IotProperties iotProperties;

    public YamlProtocolSecurityDefinitionProvider(IotProperties iotProperties) {
        this.iotProperties = iotProperties;
    }

    @Override
    public IotProperties.Protocol.FamilyDefinition getFamilyDefinition(String familyCode) {
        String normalizedFamilyCode = normalizeText(familyCode);
        if (!StringUtils.hasText(normalizedFamilyCode)) {
            return null;
        }
        return listFamilyDefinitions().values().stream()
                .filter(Objects::nonNull)
                .filter(definition -> normalizedFamilyCode.equalsIgnoreCase(normalizeText(definition.getFamilyCode())))
                .findFirst()
                .orElse(null);
    }

    @Override
    public IotProperties.Protocol.DecryptProfile getDecryptProfile(String profileCode) {
        String normalizedProfileCode = normalizeText(profileCode);
        if (!StringUtils.hasText(normalizedProfileCode)) {
            return null;
        }
        return listDecryptProfiles().values().stream()
                .filter(Objects::nonNull)
                .filter(profile -> normalizedProfileCode.equalsIgnoreCase(normalizeText(profile.getProfileCode())))
                .findFirst()
                .orElse(null);
    }

    @Override
    public Map<String, IotProperties.Protocol.FamilyDefinition> listFamilyDefinitions() {
        Map<String, IotProperties.Protocol.FamilyDefinition> configured =
                iotProperties == null || iotProperties.getProtocol() == null
                        ? null
                        : iotProperties.getProtocol().getFamilyDefinitions();
        if (configured == null || configured.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<String, IotProperties.Protocol.FamilyDefinition> normalized = new LinkedHashMap<>();
        configured.forEach((mapKey, definition) -> {
            if (definition == null) {
                return;
            }
            IotProperties.Protocol.FamilyDefinition copy = copyFamilyDefinition(definition);
            copy.setFamilyCode(resolveFamilyCode(mapKey, definition));
            normalized.put(copy.getFamilyCode(), copy);
        });
        return Map.copyOf(normalized);
    }

    @Override
    public Map<String, IotProperties.Protocol.DecryptProfile> listDecryptProfiles() {
        Map<String, IotProperties.Protocol.DecryptProfile> configured =
                iotProperties == null || iotProperties.getProtocol() == null
                        ? null
                        : iotProperties.getProtocol().getDecryptProfiles();
        if (configured == null || configured.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<String, IotProperties.Protocol.DecryptProfile> normalized = new LinkedHashMap<>();
        configured.forEach((mapKey, profile) -> {
            if (profile == null) {
                return;
            }
            IotProperties.Protocol.DecryptProfile copy = copyDecryptProfile(profile);
            copy.setProfileCode(resolveProfileCode(mapKey, profile));
            normalized.put(copy.getProfileCode(), copy);
        });
        return Map.copyOf(normalized);
    }

    private String resolveFamilyCode(String mapKey, IotProperties.Protocol.FamilyDefinition definition) {
        String familyCode = normalizeText(definition == null ? null : definition.getFamilyCode());
        if (StringUtils.hasText(familyCode)) {
            return familyCode;
        }
        return normalizeText(mapKey);
    }

    private String resolveProfileCode(String mapKey, IotProperties.Protocol.DecryptProfile profile) {
        String profileCode = normalizeText(profile == null ? null : profile.getProfileCode());
        if (StringUtils.hasText(profileCode)) {
            return profileCode;
        }
        return normalizeText(mapKey);
    }

    private IotProperties.Protocol.FamilyDefinition copyFamilyDefinition(IotProperties.Protocol.FamilyDefinition source) {
        IotProperties.Protocol.FamilyDefinition target = new IotProperties.Protocol.FamilyDefinition();
        target.setFamilyCode(normalizeText(source.getFamilyCode()));
        target.setProtocolCode(normalizeText(source.getProtocolCode()));
        target.setDisplayName(normalizeText(source.getDisplayName()));
        target.setDecryptProfileCode(normalizeText(source.getDecryptProfileCode()));
        target.setSignAlgorithm(normalizeText(source.getSignAlgorithm()));
        target.setNormalizationStrategy(normalizeText(source.getNormalizationStrategy()));
        target.setEnabled(source.getEnabled());
        return target;
    }

    private IotProperties.Protocol.DecryptProfile copyDecryptProfile(IotProperties.Protocol.DecryptProfile source) {
        IotProperties.Protocol.DecryptProfile target = new IotProperties.Protocol.DecryptProfile();
        target.setProfileCode(normalizeText(source.getProfileCode()));
        target.setAlgorithm(normalizeText(source.getAlgorithm()));
        target.setMerchantSource(normalizeText(source.getMerchantSource()));
        target.setMerchantKey(normalizeText(source.getMerchantKey()));
        target.setTransformation(normalizeText(source.getTransformation()));
        target.setSignatureSecret(normalizeText(source.getSignatureSecret()));
        target.setEnabled(source.getEnabled());
        return target;
    }

    private String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
