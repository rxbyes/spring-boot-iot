package com.ghlzm.iot.protocol.mqtt;

public interface ProtocolDecryptProfileResolver {

    ProtocolDecryptProfile resolveOrThrow(ProtocolDecryptResolveContext context);
}
