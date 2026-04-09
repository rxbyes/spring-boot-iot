package com.ghlzm.iot.protocol.mqtt;

import java.util.List;

public record ProtocolDecryptResolveContext(String appId,
                                            String protocolCode,
                                            List<String> familyCodes) {

    public ProtocolDecryptResolveContext {
        familyCodes = familyCodes == null ? List.of() : List.copyOf(familyCodes);
    }
}
