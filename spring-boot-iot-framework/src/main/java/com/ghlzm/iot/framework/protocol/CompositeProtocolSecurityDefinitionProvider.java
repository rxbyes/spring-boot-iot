package com.ghlzm.iot.framework.protocol;

import com.ghlzm.iot.framework.config.IotProperties;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Primary
@Component
public class CompositeProtocolSecurityDefinitionProvider implements ProtocolSecurityDefinitionProvider {

    private final List<ProtocolSecurityDefinitionProvider> providers;

    @Autowired
    public CompositeProtocolSecurityDefinitionProvider(PublishedProtocolSecurityDefinitionProvider publishedProvider,
                                                       YamlProtocolSecurityDefinitionProvider yamlProvider) {
        this(List.of(publishedProvider, yamlProvider));
    }

    public CompositeProtocolSecurityDefinitionProvider(List<ProtocolSecurityDefinitionProvider> providers) {
        this.providers = providers == null ? List.of() : List.copyOf(providers);
    }

    @Override
    public IotProperties.Protocol.FamilyDefinition getFamilyDefinition(String familyCode) {
        String normalizedFamilyCode = normalizeText(familyCode);
        if (!StringUtils.hasText(normalizedFamilyCode)) {
            return null;
        }
        return providers.stream()
                .map(provider -> provider.getFamilyDefinition(normalizedFamilyCode))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    @Override
    public IotProperties.Protocol.DecryptProfile getDecryptProfile(String profileCode) {
        String normalizedProfileCode = normalizeText(profileCode);
        if (!StringUtils.hasText(normalizedProfileCode)) {
            return null;
        }
        return providers.stream()
                .map(provider -> provider.getDecryptProfile(normalizedProfileCode))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    @Override
    public Map<String, IotProperties.Protocol.FamilyDefinition> listFamilyDefinitions() {
        LinkedHashMap<String, IotProperties.Protocol.FamilyDefinition> merged = new LinkedHashMap<>();
        for (ProtocolSecurityDefinitionProvider provider : providers) {
            if (provider == null) {
                continue;
            }
            provider.listFamilyDefinitions().forEach((key, value) -> {
                String normalizedKey = normalizeText(key);
                if (StringUtils.hasText(normalizedKey) && value != null) {
                    merged.putIfAbsent(normalizedKey, value);
                }
            });
        }
        return Map.copyOf(merged);
    }

    @Override
    public Map<String, IotProperties.Protocol.DecryptProfile> listDecryptProfiles() {
        LinkedHashMap<String, IotProperties.Protocol.DecryptProfile> merged = new LinkedHashMap<>();
        for (ProtocolSecurityDefinitionProvider provider : providers) {
            if (provider == null) {
                continue;
            }
            provider.listDecryptProfiles().forEach((key, value) -> {
                String normalizedKey = normalizeText(key);
                if (StringUtils.hasText(normalizedKey) && value != null) {
                    merged.putIfAbsent(normalizedKey, value);
                }
            });
        }
        return Map.copyOf(merged);
    }

    private String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
