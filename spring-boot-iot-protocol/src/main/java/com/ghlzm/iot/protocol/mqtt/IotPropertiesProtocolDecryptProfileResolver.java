package com.ghlzm.iot.protocol.mqtt;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.framework.config.IotProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;

@Component
public class IotPropertiesProtocolDecryptProfileResolver implements ProtocolDecryptProfileResolver {

    private static final String DEFAULT_PROTOCOL_CODE = "mqtt-json";

    private final IotProperties iotProperties;

    public IotPropertiesProtocolDecryptProfileResolver(IotProperties iotProperties) {
        this.iotProperties = iotProperties;
    }

    @Override
    public ProtocolDecryptProfile resolveOrThrow(ProtocolDecryptResolveContext context) {
        ProtocolDecryptProfile familyResolved = resolveByFamily(context);
        if (familyResolved != null) {
            return familyResolved;
        }
        ProtocolDecryptProfile appIdResolved = resolveByAppId(context == null ? null : context.appId());
        if (appIdResolved != null) {
            return appIdResolved;
        }
        throw new BizException("未找到 appId 对应的 decrypt profile: " + safeAppId(context));
    }

    private ProtocolDecryptProfile resolveByFamily(ProtocolDecryptResolveContext context) {
        if (context == null || context.familyCodes().isEmpty()) {
            return null;
        }
        String protocolCode = StringUtils.hasText(context.protocolCode())
                ? context.protocolCode()
                : DEFAULT_PROTOCOL_CODE;
        for (String familyCode : context.familyCodes()) {
            IotProperties.Protocol.FamilyDefinition familyDefinition =
                    iotProperties.getProtocol().getFamilyDefinitions().get(familyCode);
            if (familyDefinition == null || !isEnabled(familyDefinition.getEnabled())) {
                continue;
            }
            if (StringUtils.hasText(familyDefinition.getProtocolCode())
                    && !protocolCode.equalsIgnoreCase(familyDefinition.getProtocolCode())) {
                continue;
            }
            return resolveFamilyBoundProfileOrThrow(familyCode, familyDefinition.getDecryptProfileCode());
        }
        return null;
    }

    private ProtocolDecryptProfile resolveFamilyBoundProfileOrThrow(String familyCode, String profileCode) {
        if (!StringUtils.hasText(profileCode)) {
            throw new BizException("family 未配置 decrypt profile: " + familyCode);
        }
        IotProperties.Protocol.DecryptProfile configuredProfile =
                iotProperties.getProtocol().getDecryptProfiles().get(profileCode);
        if (configuredProfile == null) {
            throw new BizException("family 绑定的 decrypt profile 不存在: " + profileCode);
        }
        if (!isEnabled(configuredProfile.getEnabled())) {
            throw new BizException("family 绑定的 decrypt profile 未启用: " + profileCode);
        }
        return toRuntimeProfile(profileCode, configuredProfile);
    }

    private ProtocolDecryptProfile resolveByAppId(String appId) {
        if (!StringUtils.hasText(appId)) {
            return null;
        }
        for (Map.Entry<String, IotProperties.Protocol.DecryptProfile> entry
                : iotProperties.getProtocol().getDecryptProfiles().entrySet()) {
            IotProperties.Protocol.DecryptProfile configuredProfile = entry.getValue();
            if (configuredProfile == null || !isEnabled(configuredProfile.getEnabled())) {
                continue;
            }
            if (appId.equals(configuredProfile.getMerchantKey())) {
                return toRuntimeProfile(entry.getKey(), configuredProfile);
            }
        }
        return null;
    }

    private ProtocolDecryptProfile toRuntimeProfile(String mapKey,
                                                    IotProperties.Protocol.DecryptProfile configuredProfile) {
        ProtocolDecryptProfile runtimeProfile = new ProtocolDecryptProfile();
        runtimeProfile.setProfileCode(StringUtils.hasText(configuredProfile.getProfileCode())
                ? configuredProfile.getProfileCode()
                : mapKey);
        runtimeProfile.setAlgorithm(configuredProfile.getAlgorithm());
        runtimeProfile.setMerchantSource(configuredProfile.getMerchantSource());
        runtimeProfile.setMerchantKey(configuredProfile.getMerchantKey());
        runtimeProfile.setTransformation(configuredProfile.getTransformation());
        runtimeProfile.setSignatureSecret(configuredProfile.getSignatureSecret());
        runtimeProfile.setEnabled(isEnabled(configuredProfile.getEnabled()));
        return runtimeProfile;
    }

    private boolean isEnabled(Boolean enabled) {
        return !Boolean.FALSE.equals(enabled);
    }

    private String safeAppId(ProtocolDecryptResolveContext context) {
        if (context == null || !StringUtils.hasText(context.appId())) {
            return "UNKNOWN";
        }
        return context.appId();
    }
}
